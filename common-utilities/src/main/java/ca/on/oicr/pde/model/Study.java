package ca.on.oicr.pde.model;

import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Study implements Accessionable, Attributable, Name {

    private String title;
    private String swid;
    private Map<String,String> attributes;
    
    public Study(){
        attributes = Collections.EMPTY_MAP;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Map getAttributes() {
        return new HashMap(attributes);
    }

    @Override
    public void setAttributes(String attributes) {
        
        this.attributes = Splitter.on(";").omitEmptyStrings().withKeyValueSeparator("=").split(attributes);

    }
    
    @Override
    public String getAttribute(String key){
        
        return this.attributes.get(key);
        
    }
    
    @Override
    public String getName(){
        return title;
    }
    
    @Override
    public String getSwid() {
        return swid;
    }

    public void setSwid(String swid) {
        this.swid = swid;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        //
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return EqualsBuilder.reflectionEquals(this, obj);
    }
    
    

}
