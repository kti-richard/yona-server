/*******************************************************************************
 * Copyright (c) 2015 Stichting Yona Foundation This Source Code Form is subject to the terms of the Mozilla Public License, v.
 * 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *******************************************************************************/
package nu.yona.server.subscriptions.entities;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Transient;

import nu.yona.server.crypto.Decryptor;
import nu.yona.server.crypto.Encryptor;
import nu.yona.server.goals.entities.Goal;

@Entity
public class BuddyConnectRequestMessage extends BuddyConnectMessage
{
	@Transient
	private String nickname;
	private byte[] nicknameCiphertext;

	private BuddyAnonymized.Status status = BuddyAnonymized.Status.NOT_REQUESTED;

	// Default constructor is required for JPA
	public BuddyConnectRequestMessage()
	{
		super();
	}

	private BuddyConnectRequestMessage(UUID id, UUID userID, UUID loginID, Set<UUID> goalIDs, String nickname, String message,
			UUID buddyID)
	{
		super(id, loginID, userID, message, buddyID);
		if (userID == null)
		{
			throw new IllegalArgumentException("requestingUserID cannot be null");
		}
		this.nickname = nickname;
	}

	public String getNickname()
	{
		return nickname;
	}

	public boolean isAccepted()
	{
		return status == BuddyAnonymized.Status.ACCEPTED;
	}

	public boolean isRejected()
	{
		return status == BuddyAnonymized.Status.REJECTED;
	}

	public void setStatus(BuddyAnonymized.Status status)
	{
		this.status = status;
	}

	public BuddyAnonymized.Status getStatus()
	{
		return this.status;
	}

	public static BuddyConnectRequestMessage createInstance(UUID requestingUserID, UUID requestingUserLoginID, Set<Goal> goals,
			String nickname, String message, UUID buddyID)
	{
		return new BuddyConnectRequestMessage(UUID.randomUUID(), requestingUserID, requestingUserLoginID,
				goals.stream().map(g -> g.getID()).collect(Collectors.toSet()), nickname, message, buddyID);
	}

	@Override
	public void encrypt(Encryptor encryptor)
	{
		super.encrypt(encryptor);
		nicknameCiphertext = encryptor.encrypt(nickname);
	}

	@Override
	public void decrypt(Decryptor decryptor)
	{
		super.decrypt(decryptor);
		nickname = decryptor.decryptString(nicknameCiphertext);
	}

	@Override
	public boolean canBeDeleted()
	{
		return this.status == BuddyAnonymized.Status.ACCEPTED || this.status == BuddyAnonymized.Status.REJECTED;
	}
}
