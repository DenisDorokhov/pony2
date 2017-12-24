package net.dorokhov.pony.api.common;

import java.io.Serializable;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

import static net.dorokhov.pony.api.common.SearchableEntity.ANALYZER;

@AnalyzerDef(name = ANALYZER,
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
                @TokenFilterDef(factory = StandardFilterFactory.class)
        })
abstract public class SearchableEntity<T extends Serializable> extends BaseEntity<T> {
    public static final String ANALYZER = "noStopWordsAnalyzer";
}
