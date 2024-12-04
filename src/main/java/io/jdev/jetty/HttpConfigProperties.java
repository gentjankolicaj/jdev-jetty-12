package io.jdev.jetty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author gentjan kolicaj
 * @Date: 12/4/24 2:34 PM
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes(value = {@JsonSubTypes.Type(HttpProperties.class),
    @JsonSubTypes.Type(HttpsProperties.class)})
public abstract class HttpConfigProperties {

}
