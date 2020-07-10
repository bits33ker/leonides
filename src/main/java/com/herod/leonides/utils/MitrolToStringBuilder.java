/**
 *
 */
package com.herod.leonides.utils;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


/**
 * @author nzuidwijk on 19/5/2015
 */
public class MitrolToStringBuilder {

    public static String toString(Object object) {
        return ReflectionToStringBuilder.toString(object, ToStringStyle.JSON_STYLE);
    }
    
    public static String toString(Object object, String... excludedFieldNames) {
        return new ReflectionToStringBuilder(object, ToStringStyle.JSON_STYLE)
        			.setExcludeFieldNames(excludedFieldNames)
        			.toString();        
    }
}
