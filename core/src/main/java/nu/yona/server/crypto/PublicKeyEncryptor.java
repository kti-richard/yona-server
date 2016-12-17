/*******************************************************************************
 * Copyright (c) 2015, 2016 Stichting Yona Foundation This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *******************************************************************************/
package nu.yona.server.crypto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;

public class PublicKeyEncryptor implements Encryptor
{
	/**
	 * Maximum payload in a single RSA-encrypted message with OAEP padding, see http://stackoverflow.com/a/11750658/4353482
	 */
	private static final int SMALL_PAYLOAD_MAX_LENGTH = 86;
	private final PublicKey publicKey;

	private PublicKeyEncryptor(PublicKey publicKey)
	{
		if (publicKey == null)
		{
			throw new IllegalArgumentException("publicKey cannot be null");
		}

		this.publicKey = publicKey;
	}

	public static PublicKeyEncryptor createInstance(PublicKey publicKey)
	{
		return new PublicKeyEncryptor(publicKey);
	}

	@Override
	public byte[] encrypt(byte[] plaintext)
	{
		try
		{
			if (plaintext == null)
			{
				return null;
			}
			Cipher encryptCipher = Cipher.getInstance(PublicKeyUtil.CIPHER_TYPE);
			encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

			return CryptoUtil.encrypt(PublicKeyUtil.CURRENT_SMALL_PLAINTEXT_CRYPTO_VARIANT_NUMBER, SMALL_PAYLOAD_MAX_LENGTH,
					PublicKeyUtil.CURRENT_LARGE_PLAINTEXT_CRYPTO_VARIANT_NUMBER, encryptCipher, plaintext);
		}
		catch (GeneralSecurityException e)
		{
			throw CryptoException.encryptingData(e);
		}
	}

	@Override
	public byte[] encrypt(String plaintext)
	{
		return (plaintext == null) ? null : encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public byte[] encrypt(UUID plaintext)
	{
		return (plaintext == null) ? null : encrypt(plaintext.toString());
	}

	@Override
	public byte[] encrypt(long plaintext)
	{
		return encrypt(Long.toString(plaintext));
	}

	@Override
	public byte[] encrypt(Set<UUID> plaintext)
	{
		try
		{
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);
			dataStream.writeInt(plaintext.size());
			plaintext.stream().forEach(id -> writeUUID(dataStream, id));

			return encrypt(byteStream.toByteArray());
		}
		catch (IOException e)
		{
			throw CryptoException.encryptingData(e);
		}
	}

	private void writeUUID(DataOutputStream dataStream, UUID id)
	{
		try
		{
			dataStream.writeLong(id.getMostSignificantBits());
			dataStream.writeLong(id.getLeastSignificantBits());
		}
		catch (IOException e)
		{
			throw CryptoException.writingData(e);
		}
	}
}
