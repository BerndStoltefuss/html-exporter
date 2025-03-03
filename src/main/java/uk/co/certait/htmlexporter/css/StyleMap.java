/**
 * Copyright (C) 2012 alanhay <alanhay99@hotmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.certait.htmlexporter.css;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;

/**
 * 
 * @author alanhay
 * 
 */
public class StyleMap {
	private static final String CLASS_PREFIX = ".";

	Map<String, Style> styles;
	Map<Integer, Style> cache;
	Map<String, Style> inlineCache;
	private StyleGenerator generator;

	public StyleMap(Map<String, Style> styles) {
		this.styles = styles != null ? styles : new HashMap<String, Style>(1000);
		this.cache = new HashMap<Integer, Style>(50000);
		this.inlineCache = new HashMap<String, Style>(1000);
		generator = new StyleGenerator();
	}

	public Style getStyleForElement(Element element) {
		int elHash = getRelevantHashForElement(element);
		Style cached = cache.get(elHash);
		if (cached != null) return cached;
		Style s = StyleMerger.mergeStyles(getAllStyles(element).toArray(new Style[0]));
		cache.put(elHash, s);
		return s;
	}
	
	protected List<Style> getAllStyles(Element element) {
		
		List<Style> styles = new ArrayList<>();

		if (getStyleForTag(element) != null) {
			styles.add(getStyleForTag(element));
		}

		if (!getStylesForClass(element).isEmpty()) {
			List<Style> classStyles = getStylesForClass(element);
			styles.addAll(classStyles);
		}

		Optional<Style> inlineStyle = getInlineStyle(element);

		if (inlineStyle.isPresent()) {
			styles.add(inlineStyle.get());
		}
		
		//recursive call for each parent element (closest first)
		//get any applicable styles and insert at start of list to
		//preserve priority
		if (element.parent()!=null) styles.add(0, getStyleForElement(element.parent()));
		
		return styles;		
	}

	private Style getStyleForTag(Element element) {
		return styles.get(element.tagName().toLowerCase());
	}

	private List<Style> getStylesForClass(Element element) {
		List<Style> classStyles = new ArrayList<Style>();

		if (StringUtils.isNotEmpty(element.className())) {
			String[] classNames = element.className().split(" ");

			for (String className : classNames) {
				String qualifiedClassName = CLASS_PREFIX + className.trim().toLowerCase();
				String fullyQualifiedClassName = element.tagName().toLowerCase() + CLASS_PREFIX + className.trim().toLowerCase();

				if (styles.containsKey(qualifiedClassName)) {
					classStyles.add(styles.get(qualifiedClassName));
				}
				if (styles.containsKey(fullyQualifiedClassName)) {
					classStyles.add(styles.get(fullyQualifiedClassName));
				}
				
			}
		}

		return classStyles;
	}

	protected Optional<Style> getInlineStyle(Element element) {
		if (element.hasAttr("style")) {
			Style cached = inlineCache.get(element.attr("style"));
			if (cached!=null) return Optional.of(cached);
		}
		
		List<Style> styles = new ArrayList<>();

		if (element.hasAttr("style")) {
			CSSDeclarationList cssStyles = CSSReaderDeclarationList.readFromString(element.attr("style"),
					ECSSVersion.LATEST);
			ICommonsList<CSSDeclaration> declarations = cssStyles.getAllDeclarations();

			for (CSSDeclaration declaration : declarations) {
				styles.add(generator.createStyle(declaration));
			}
		}
		Style s = StyleMerger.mergeStyles(styles.toArray(new Style[0]));
		if (element.hasAttr("style")) inlineCache.put(element.attr("style"), s);
		return Optional.of(s);
	}
	
	private int getRelevantHashForElement(Element element) {
		int hash = 7;
		
		if (element == null) return hash;
		
		String tagname = element.tagName().toLowerCase();
		String classname = element.className();
		String inlinestyle = (element.hasAttr("style"))?element.attr("style"):null;
		String width = (element.hasAttr("width"))?element.attr("width"):null;
		int parentHash = (element.parent()!=null)?element.parent().hashCode():0;
		
		 hash = 31 * hash + tagname.hashCode();
		 hash = 31 * hash + (classname == null ? 0 : classname.hashCode());
		 hash = 31 * hash + (inlinestyle == null ? 0 : inlinestyle.hashCode());
		 hash = 31 * hash + (width == null ? 0 : width.hashCode());
		 hash = 31 * hash + parentHash;
		 return hash;
	}
}
