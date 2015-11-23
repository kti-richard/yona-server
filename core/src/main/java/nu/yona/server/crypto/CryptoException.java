package nu.yona.server.crypto;

import nu.yona.server.exceptions.YonaException;

public class CryptoException extends YonaException
{
	private static final long serialVersionUID = -1379944976933747626L;

	protected CryptoException(String messageId, Object... parameters)
	{
		super(messageId, parameters);
	}

	public CryptoException(Throwable t, String messageId, Object... parameters)
	{
		super(t, messageId, parameters);
	}

	public static CryptoException noActiveCryptoSession(Thread thread)
	{
		return new CryptoException("error.no.active.crypto.session", thread);
	}

	public static CryptoException gettingCipher(String name)
	{
		return new CryptoException("error.getting.cipher", name);
	}

	public static CryptoException encryptingData(Throwable e)
	{
		return new CryptoException(e, "error.encrypting.data");
	}

	public static CryptoException decryptingData(Throwable e)
	{
		return new CryptoException(e, "error.decrypting.data");
	}

	public static CryptoException creatingSecretKey(Throwable e)
	{
		return new CryptoException(e, "error.creating.secret.key");
	}

	public static CryptoException initializationVectorNotSet()
	{
		return new CryptoException("error.initialization.vector.not.set");
	}

	public static CryptoException initializationVectorWrongSize(int length, int initializationVectorLength)
	{
		return new CryptoException("error.initialization.vector.wrong.size", length, initializationVectorLength);
	}

	public static CryptoException initializationVectorParameterNull()
	{
		return new CryptoException("error.initialization.vector.parameter.null");
	}

	public static CryptoException initializationVectorOverwrite()
	{
		return new CryptoException("error.initialization.vector.overwrite");
	}

	public static CryptoException gettingRandomInstance(Throwable e)
	{
		return new CryptoException(e, "error.getting.random.instance");
	}

	public static CryptoException readingUUID(Throwable e)
	{
		return new CryptoException(e, "error.reading.uuid");
	}

	public static CryptoException writingData(Throwable e)
	{
		return new CryptoException(e, "error.writing.data");
	}

	public static CryptoException generatingKeyPair(Throwable e)
	{
		return new CryptoException(e, "error.generating.key.pair");
	}

	public static CryptoException encodingPrivateKey(Throwable e)
	{
		return new CryptoException(e, "error.encoding.private.key");
	}

	public static CryptoException decodingPrivateKey(Throwable e)
	{
		return new CryptoException(e, "error.decoding.private.key");
	}

	public static CryptoException encodingPublicKey(Throwable e)
	{
		return new CryptoException(e, "error.encoding.public.key");
	}

	public static CryptoException decodingPublicKey(Throwable e)
	{
		return new CryptoException(e, "error.decoding.public.key");
	}
}
