package net.dorokhov.pony2.core.library.service.search;

import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

import java.io.Reader;
import java.util.Map;

@SuppressWarnings("unused")
public class TransliterationMappingCharFilterFactory extends CharFilterFactory {

    private static final NormalizeCharMap MAPPING;
    static {
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        TransliterationMappingRegistry.mapping().forEach(builder::add);
        MAPPING = builder.build();
    }

    public TransliterationMappingCharFilterFactory() {
        super();
    }

    public TransliterationMappingCharFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public Reader create(Reader reader) {
        return new MappingCharFilter(MAPPING, reader);
    }
}
