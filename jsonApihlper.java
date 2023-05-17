package org.unitime.timetable.api;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.CacheMode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.unitime.timetable.security.SessionContext;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;

/**
 * @author Tomas Muller
 */
public class JsonApiHelper extends AbstractApiHelper {
	protected Gson iGson;
	private static final String DATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private static final String CACHE_CONTROL = "Cache-Control";

	public JsonApiHelper(HttpServletRequest request, HttpServletResponse response, SessionContext context, CacheMode cacheMode) {
		super(request, response, context, cacheMode);
	}

	protected Gson createGson() {
		return new GsonBuilder()
				.registerTypeAdapter(java.sql.Timestamp.class, new JsonSerializer<java.sql.Timestamp>() {
					@Override
					public JsonElement serialize(java.sql.Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
						return new JsonPrimitive(new SimpleDateFormat(DATE).format(src));
					}
				})
				.registerTypeAdapter(java.sql.Timestamp.class, new JsonDeserializer<java.sql.Timestamp>() {
					@Override
					public java.sql.Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						try {
							return new java.sql.Timestamp(new SimpleDateFormat(DATE).parse(json.getAsJsonPrimitive().getAsString()).getTime());
						} catch (ParseException e) {
							throw new JsonParseException(e.getMessage(), e);
						}
					}
				})
				.registerTypeAdapter(java.sql.Date.class, new JsonSerializer<java.sql.Date>() {
					@Override
					public JsonElement serialize(java.sql.Date src, Type typeOfSrc, JsonSerializationContext context) {
						return new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd").format(src));
					}
				})
				.registerTypeAdapter(java.sql.Date.class, new JsonDeserializer<java.sql.Date>() {
					@Override
					public java.sql.Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						try {
							return new java.sql.Date(new SimpleDateFormat(DATE).parse(json.getAsJsonPrimitive().getAsString()).getTime());
						} catch (ParseException e) {
							throw new JsonParseException(e.getMessage(), e);
						}
					}
				})
				.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
					@Override
					public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
						return new JsonPrimitive(new SimpleDateFormat(DATE).format(src));
					}
				})
				.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
					@Override
					public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						try {
							return new SimpleDateFormat(DATE).parse(json.getAsJsonPrimitive().getAsString());
						} catch (ParseException e) {
							throw new JsonParseException(e.getMessage(), e);
						}
					}
				})
				.registerTypeAdapter(DateTime.class, new JsonSerializer<DateTime>() {
					@Override
					public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
						return new JsonPrimitive(src.toString(DATE));
					}
				})
				.registerTypeAdapter(DateTime.class, new JsonDeserializer<DateTime>() {
					@Override
					public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						return new DateTime(json.getAsJsonPrimitive().getAsString(), DateTimeZone.UTC);
					}
				})
				.setFieldNamingStrategy(new FieldNamingStrategy() {
					Pattern iPattern = Pattern.compile("i([A-Z])(.*)");
					@Override
					public String translateName(Field f) {
						Matcher matcher = iPattern.matcher(f.getName());
						if (matcher.matches())
							return matcher.group(1).toLowerCase() + matcher.group(2);
						else
							return f.getName();
					}
				})
				.setPrettyPrinting().create();
	}

	@Override
	public <P> P getRequest(Type requestType) throws IOException {
		if (iGson == null) iGson = createGson();
		JsonReader reader = new JsonReader(iRequest.getReader());
		try {
			return iGson.fromJson(reader, requestType);
		} finally {
			reader.close();
		}
	}

	@Override
	public <R> void setResponse(R response) throws IOException {
		if (iGson == null) iGson = createGson();
		iResponse.setContentType("application/json");
		iResponse.setCharacterEncoding("UTF-8");
		iResponse.setHeader("Pragma", "no-cache" );
		iResponse.addHeader(CACHE_CONTROL, "must-revalidate" );
		iResponse.addHeader(CACHE_CONTROL, "no-cache" );
		iResponse.addHeader(CACHE_CONTROL, "no-store" );
		iResponse.setDateHeader("Date", new Date().getTime());
		iResponse.setDateHeader("Expires", 0);
		iResponse.setHeader("Content-Disposition", "attachment; filename=\"response.json\"" );
		Writer writer = iResponse.getWriter();
		try {
			writer.write(iGson.toJson(response));
		} finally {
			writer.flush();
			writer.close();
		}
	}

	@Override
	public void sendError(int code, String message) throws IOException {
		try {
			iResponse.setStatus(code);
			setResponse(new ErrorMessage(code, message, this));
		} catch (Throwable t) {
			iResponse.sendError(code, message);
		}
	}

	@Override
	public void sendError(int code) throws IOException {
		try {
			iResponse.setStatus(code);
			setResponse(new ErrorMessage(code, this));
		} catch (Throwable t) {
			iResponse.sendError(code);
		}
	}

	@Override
	public void sendError(int code, Throwable error) throws IOException {
		try {
			iResponse.setStatus(code);
			setResponse(new ErrorMessage(code, error, this));
		} catch (Throwable t) {
			iResponse.sendError(code, error.getMessage());
		}
	}
}
