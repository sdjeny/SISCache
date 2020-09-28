package org.sdjen.download.cache_sis.json;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsonUtil {
	// private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final ObjectMapper mapper;

	public static ObjectMapper getMapper() {
		return mapper;
	}

	static {
		// SimpleDateFormat dateFormat = new
		// SimpleDateFormat(DEFAULT_DATE_FORMAT);

		mapper = new ObjectMapper();
		// mapper.setDateFormat(dateFormat);
		// mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector()
		// {
		// @Override
		// public Object findSerializer(Annotated a) {
		// if (a instanceof AnnotatedMethod) {
		// AnnotatedElement m = a.getAnnotated();
		// DateTimeFormat an = m.getAnnotation(DateTimeFormat.class);
		// if (an != null) {
		// if (!DEFAULT_DATE_FORMAT.equals(an.pattern())) {
		// return new JsonDateSerializer(an.pattern());
		// }
		// }
		// }
		// return super.findSerializer(a);
		// }
		// });
	}

	public static synchronized String toJson(Object obj) throws JsonProcessingException {
		return mapper.writeValueAsString(obj);
	}

	public static synchronized <T> T toObject(String json, Class<T> clazz)
			throws JsonMappingException, JsonProcessingException {
		return mapper.readValue(json, clazz);
	}

	public static class JsonDateSerializer extends JsonSerializer<Date> {
		private SimpleDateFormat dateFormat;

		public JsonDateSerializer(String format) {
			dateFormat = new SimpleDateFormat(format);
		}

		@Override
		public void serialize(Date date, JsonGenerator gen, SerializerProvider provider)
				throws IOException, JsonProcessingException {
			String value = dateFormat.format(date);
			gen.writeString(value);
		}
	}
}
