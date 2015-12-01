package nu.yona.server.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import nu.yona.server.exceptions.YonaException;

/**
 * This class contains the mapping for the different exceptions and how they should be mapped to an http response
 * 
 * @author pgussow
 */
@ControllerAdvice
public class GlobalExceptionMapping
{
	private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapping.class.getName());

	/** The source for the messages to use */
	@Autowired
	private MessageSource msgSource;

	/**
	 * This method generically handles the illegal argument exceptions. They are translated into nice ResponseMessage objects so
	 * the response data is properly organized and JSON parseable.
	 * 
	 * @param ide The exception
	 * @return The response object to return.
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ResponseMessageDTO handleOtherException(Exception exception)
	{
		LOGGER.log(Level.SEVERE, "Unhandled exception", exception);

		return new ResponseMessageDTO(ResponseMessageType.ERROR, null, exception.getMessage());
	}

	/**
	 * This method generically handles the illegal argument exceptions. They are translated into nice ResponseMessage objects so
	 * the response data is properly organized and JSON parseable.
	 * 
	 * @param ide The exception
	 * @return The response object to return.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ResponseMessageDTO handleIllegalArgumentException(IllegalArgumentException exception)
	{
		LOGGER.log(Level.INFO, "Unhandled exception", exception);

		return new ResponseMessageDTO(ResponseMessageType.ERROR, null, exception.getMessage());
	}

	/**
	 * This method generically handles the Yona exceptions. They are translated into nice ResponseMessage objects so the response
	 * data is properly organized and JSON parseable.
	 * 
	 * @param ide The exception
	 * @return The response object to return.
	 */
	@ExceptionHandler(YonaException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ResponseMessageDTO handleYonaException(YonaException exception)
	{
		LOGGER.log(Level.INFO, "Unhandled exception", exception);

		return new ResponseMessageDTO(ResponseMessageType.ERROR, exception.getMessageId(),
				exception.getLocalizedMessage(msgSource));
	}
}
