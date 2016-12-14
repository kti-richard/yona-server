/*******************************************************************************
 * Copyright (c) 2015, 2016 Stichting Yona Foundation This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *******************************************************************************/
package nu.yona.server.subscriptions.service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import nu.yona.server.Constants;
import nu.yona.server.exceptions.MobileNumberConfirmationException;
import nu.yona.server.goals.service.GoalDTO;
import nu.yona.server.subscriptions.entities.User;
import nu.yona.server.util.TimeUtil;

@JsonRootName("user")
public class UserDTO
{
	public static final String BUDDIES_REL_NAME = "buddies";
	public static final String GOALS_REL_NAME = "goals";

	private UUID id;
	private final String firstName;
	private final String lastName;
	private final String emailAddress;
	private final String mobileNumber;
	private final boolean isMobileNumberConfirmed;
	private final UserPrivateDTO privateData;
	private final Optional<LocalDateTime> creationTime;

	/*
	 * Only intended for test purposes.
	 */
	private UserDTO(UUID id, LocalDateTime creationTime, String firstName, String lastName, String nickname, String mobileNumber,
			boolean isConfirmed, UUID namedMessageSourceId, UUID namedMessageDestinationId, UUID anonymousMessageSourceId,
			UUID anonymousMessageDestinationId, Set<GoalDTO> goals, Set<UUID> buddyIds,
			Function<Set<UUID>, Set<BuddyDTO>> buddyIdToDTOMapper, UUID userAnonymizedId, VPNProfileDTO vpnProfile)
	{
		this(id, firstName, lastName, null, mobileNumber, Optional.of(creationTime), isConfirmed,
				new UserPrivateDTO(nickname, namedMessageSourceId, namedMessageDestinationId, anonymousMessageSourceId,
						anonymousMessageDestinationId, goals, buddyIds, buddyIdToDTOMapper, userAnonymizedId, vpnProfile));
	}

	private UserDTO(UUID id, String firstName, String lastName, String mobileNumber, LocalDateTime creationTime,
			boolean isConfirmed)
	{
		this(id, firstName, lastName, null, mobileNumber, Optional.of(creationTime), isConfirmed, null);
	}

	@JsonCreator
	public UserDTO(@JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
			@JsonProperty("emailAddress") String emailAddress, @JsonProperty("mobileNumber") String mobileNumber,
			@JsonUnwrapped UserPrivateDTO privateData)
	{
		this(null, firstName, lastName, emailAddress, mobileNumber, Optional.empty(), false /* default value, ignored */,
				privateData);
	}

	private UserDTO(UUID id, String firstName, String lastName, String emailAddress, String mobileNumber,
			Optional<LocalDateTime> creationTime, boolean isMobileNumberConfirmed, UserPrivateDTO privateData)
	{
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.emailAddress = emailAddress;
		this.mobileNumber = mobileNumber;
		this.creationTime = creationTime;
		this.isMobileNumberConfirmed = isMobileNumberConfirmed;
		this.privateData = privateData;
	}

	@JsonIgnore
	public UUID getId()
	{
		return id;
	}

	@JsonIgnore
	public void setUserId(UUID id)
	{
		this.id = id;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	@JsonInclude(Include.NON_EMPTY)
	public String getEmailAddress()
	{
		return emailAddress;
	}

	public String getMobileNumber()
	{
		return mobileNumber;
	}

	@JsonProperty("creationTime")
	@JsonFormat(pattern = Constants.ISO_DATE_PATTERN)
	public Optional<ZonedDateTime> getCreationTimeAsZonedDateTime()
	{
		return creationTime.map(TimeUtil::toUtcZonedDateTime);
	}

	public Optional<LocalDateTime> getCreationTime()
	{
		return creationTime;
	}

	@JsonIgnore
	public boolean isMobileNumberConfirmed()
	{
		return isMobileNumberConfirmed;
	}

	@JsonUnwrapped
	public UserPrivateDTO getPrivateData()
	{
		return privateData;
	}

	User createUserEntity()
	{
		return User.createInstance(firstName, lastName, privateData.getNickname(), mobileNumber,
				privateData.getVpnProfile().getVpnPassword(),
				privateData.getGoals().stream().map(g -> g.createGoalEntity()).collect(Collectors.toSet()));
	}

	User updateUser(User originalUserEntity)
	{
		originalUserEntity.setFirstName(firstName);
		originalUserEntity.setLastName(lastName);
		originalUserEntity.setMobileNumber(mobileNumber);
		originalUserEntity.setNickname(privateData.getNickname());

		return originalUserEntity;
	}

	/**
	 * Creates a {@link UserDTO} with the public data of the user, if the user is not {@code null}. This method is provided
	 * because the related user may be removed in the meantime. In that case the passed {@code userEntity} will be {@code null}
	 * and the method returns {@code null}.
	 * 
	 * @param userEntity
	 * @return
	 */
	public static Optional<UserDTO> createInstance(Optional<User> userEntity)
	{
		return userEntity.map(ue -> createInstance(ue));
	}

	/**
	 * Creates a {@link UserDTO} with the public data of the user. Please use {@link #createInstance(User)} if {@code userEntity}
	 * may be {@code null}. Use {@link #createInstanceWithPrivateData(User)} to include private data of the user.
	 * 
	 * @param userEntity
	 * @return
	 */
	static UserDTO createInstance(User userEntity)
	{
		if (userEntity == null)
		{
			throw new IllegalArgumentException("userEntity cannot be null");
		}
		return new UserDTO(userEntity.getId(), userEntity.getFirstName(), userEntity.getLastName(), userEntity.getMobileNumber(),
				userEntity.getCreationTime(), userEntity.isMobileNumberConfirmed());
	}

	static UserDTO createInstanceWithPrivateData(User userEntity, Function<Set<UUID>, Set<BuddyDTO>> buddyIdToDTOMapper)
	{
		return new UserDTO(userEntity.getId(), userEntity.getCreationTime(), userEntity.getFirstName(), userEntity.getLastName(),
				userEntity.getNickname(), userEntity.getMobileNumber(), userEntity.isMobileNumberConfirmed(),
				userEntity.getNamedMessageSource().getId(), userEntity.getNamedMessageDestination().getId(),
				userEntity.getAnonymousMessageSource().getId(), userEntity.getAnonymousMessageSource().getDestination().getId(),
				UserAnonymizedDTO.getGoalsIncludingHistoryItems(userEntity.getAnonymized()), getBuddyIds(userEntity),
				buddyIdToDTOMapper, userEntity.getUserAnonymizedId(), VPNProfileDTO.createInstance(userEntity));
	}

	private static Set<UUID> getBuddyIds(User userEntity)
	{
		return userEntity.getBuddies().stream().map(b -> b.getId()).collect(Collectors.toSet());
	}

	public void assertMobileNumberConfirmed()
	{
		if (!isMobileNumberConfirmed)
		{
			throw MobileNumberConfirmationException.notConfirmed(mobileNumber);
		}
	}
}
