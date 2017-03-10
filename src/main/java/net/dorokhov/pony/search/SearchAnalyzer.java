package net.dorokhov.pony.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class SearchAnalyzer extends Analyzer {
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		StandardTokenizer src = new StandardTokenizer();
		TokenStream filter = new LowerCaseFilter(new StandardFilter(src));
		return new Analyzer.TokenStreamComponents(src, filter);
	}
}
