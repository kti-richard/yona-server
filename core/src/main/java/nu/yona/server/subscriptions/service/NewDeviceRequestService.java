/*******************************************************************************
 * Copyright (c) 2016, 2017 Stichting Yona Foundation This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *******************************************************************************/
package nu.yona.server.subscriptions.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nu.yona.server.properties.YonaProperties;
import nu.yona.server.subscriptions.entities.NewDeviceRequest;
import nu.yona.server.subscriptions.entities.User;
import nu.yona.server.subscriptions.entities.UserRepository;
import nu.yona.server.util.TimeUtil;

@Service
public class NewDeviceRequestService
{
	private static final Logger logger = LoggerFactory.getLogger(NewDeviceRequestService.class);

	@Autowired
	private UserService userService;

	@Autowired(required = false)
	private UserRepository userRepository;

	@Autowired
	private YonaProperties yonaProperties;

	@Transactional
	public NewDeviceRequestDto setNewDeviceRequestForUser(UUID userId, String yonaPassword, String newDeviceRequestPassword)
	{
		User userEntity = userService.getValidatedUserbyId(userId);

		NewDeviceRequest newDeviceRequestEntity = NewDeviceRequest.createInstance(yonaPassword);
		newDeviceRequestEntity.encryptYonaPassword(newDeviceRequestPassword);

		userEntity.setNewDeviceRequest(newDeviceRequestEntity);

		logger.info("User with mobile number '{}' and ID '{}' set a new device request", userEntity.getMobileNumber(),
				userEntity.getId());
		User.getRepository().save(userEntity);
		return NewDeviceRequestDto.createInstanceWithoutPassword();
	}

	@Transactional
	public NewDeviceRequestDto getNewDeviceRequestForUser(UUID userId, Optional<String> newDeviceRequestPassword)
	{
		User userEntity = userService.getValidatedUserbyId(userId);
		NewDeviceRequest newDeviceRequestEntity = userEntity.getNewDeviceRequest();

		if (newDeviceRequestEntity == null)
		{
			throw DeviceRequestException.noDeviceRequestPresent(userEntity.getMobileNumber());
		}

		if (isExpired(newDeviceRequestEntity))
		{
			throw DeviceRequestException.deviceRequestExpired(userEntity.getMobileNumber());
		}

		if (newDeviceRequestPassword.isPresent())
		{
			newDeviceRequestEntity.decryptYonaPassword(newDeviceRequestPassword.get(), userEntity.getMobileNumber());
			logger.info("User with mobile number '{}' and ID '{}' fetched the new device request", userEntity.getMobileNumber(),
					userEntity.getId());
			return NewDeviceRequestDto.createInstanceWithPassword(newDeviceRequestEntity);
		}
		else
		{
			logger.info("User with mobile number '{}' and ID '{}' verified the existence of new device request",
					userEntity.getMobileNumber(), userEntity.getId());
			return NewDeviceRequestDto.createInstanceWithoutPassword();
		}
	}

	@Transactional
	public void clearNewDeviceRequestForUser(UUID userId)
	{
		User user = userService.getValidatedUserbyId(userId);

		NewDeviceRequest existingNewDeviceRequestEntity = user.getNewDeviceRequest();
		if (existingNewDeviceRequestEntity != null)
		{
			removeNewDeviceRequest(user, "User with mobile number '{}' and ID '{}' cleared the new device request");
		}
	}

	@Transactional
	public int deleteAllExpiredRequests()
	{
		Set<User> users = userRepository.findAllWithExpiredNewDeviceRequests(TimeUtil.utcNow().minus(getExpirationTime()));
		users.forEach(u -> removeNewDeviceRequest(u,
				"New device request for user with mobile number '{}' and ID '{}' was cleared because it was expired"));
		return users.size();
	}

	private void removeNewDeviceRequest(User user, String logMessage)
	{
		logger.info(logMessage, user.getMobileNumber(), user.getId());
		user.clearNewDeviceRequest();
		userRepository.save(user);
	}

	private Duration getExpirationTime()
	{
		return yonaProperties.getSecurity().getNewDeviceRequestExpirationTime();
	}

	private boolean isExpired(NewDeviceRequest newDeviceRequestEntity)
	{
		LocalDateTime creationTime = newDeviceRequestEntity.getCreationTime();
		return creationTime.plus(getExpirationTime()).isBefore(TimeUtil.utcNow());
	}
}
