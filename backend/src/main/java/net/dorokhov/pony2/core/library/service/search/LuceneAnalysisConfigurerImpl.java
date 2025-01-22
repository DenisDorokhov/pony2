package net.dorokhov.pony2.core.library.service.search;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

@SuppressWarnings("unused")
public class LuceneAnalysisConfigurerImpl implements LuceneAnalysisConfigurer {
    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer("ponyAnalyzer").custom()
                .tokenizer(WhitespaceTokenizerFactory.class)
                .tokenFilter(ASCIIFoldingFilterFactory.class)
                .charFilter(TransliterationMappingCharFilterFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class);
    }
}
