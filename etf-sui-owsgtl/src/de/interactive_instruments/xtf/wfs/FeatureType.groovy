/**
 * Copyright 2010-2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.interactive_instruments.xtf.wfs;

import java.util.List;

import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName
import org.w3c.dom.Node;

import com.eviware.soapui.support.XmlHolder;

import de.interactive_instruments.xtf.*;
import de.interactive_instruments.xtf.exceptions.*;

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
public class FeatureType implements TransferableRequestParameter { 

	private QName qname;
	private String defaultSRS;
	private String[] otherSRS;
	private int numberOfFeaturesInDatabase;
	private Bbox bbox;
	
	
	private List <FeatureTypeProperty[]> properties = new ArrayList<FeatureTypeProperty[]>();
	private List<OutputFormat> outputFormats = new ArrayList<OutputFormat>();
	@XmlTransient
	private int currentOutputFormatIndex;

	// NamespaceHolders for ever OutputFormat
	private List<NamespaceHolder> nsHolder = new ArrayList<NamespaceHolder>();
	
	public FeatureType() { 
		// no-arg default ctor for JAXB
		currentOutputFormatIndex=0;
	}
	
	public FeatureType(String namespaceURI, String localPart, String prefix) {
		this.qname = new QName(namespaceURI, localPart, prefix);
		currentOutputFormatIndex=0;
	}
	
	public QName getQName() {
		return qname;
	}
	
	/*
	 * Return namespace and Prefix
	 */
	public String getPrefixAndName() {
		return qname.getPrefix()+":"+qname.getLocalPart();
	}
	public String getNamespaceURI() { return qname.getNamespaceURI(); }
	public String getPrefix() { return qname.getPrefix(); }
	
	@Deprecated
	public String getName() { return getPrefixAndName(); }
	public String getNamespacePrefix() { return getPrefix(); }
	public String getNamespace() { return getNamespaceURI(); }
	
	public void setDefaultSRS(String defaultSRS) { this.defaultSRS = defaultSRS; }
	public String getDefaultSRS() { return defaultSRS; }
	
	public void setOtherSRS(String[] otherSRS) { this.otherSRS = otherSRS; }
	public String[] getOtherSRS() { return otherSRS; }
	
	public void setNumberOfFeaturesInDatabase(int numberOfFeatures) {
		this.numberOfFeaturesInDatabase=numberOfFeatures;
	}
	public int getNumberOfFeaturesInDatabase() { return numberOfFeaturesInDatabase; }
	
	public void setBBOX(Bbox bbox) { this.bbox=bbox;}
	public Bbox getBBOX() { return bbox; }
	
	public void useOutputFormat(OutputFormat outputFormat) {
		currentOutputFormatIndex=0;
		for(of in outputFormats) {
			if(of==outputFormat) {
				return;
			}
			currentOutputFormatIndex++;
		}
		currentOutputFormatIndex=0;
		throw new FatalInternalException(this, "The FeatureType "+
			this.getPrefixAndName()+" does not support OutputFormat "+outputFormat);
	}
	
	public final List<OutputFormat> getOutputFormats() {
		return this.outputFormats;
	}
		
	public final String getCurrentOutputFormat() { 
		return outputFormats[currentOutputFormatIndex];
	}
	
	/*
	 * Returns the properties for the currently set GML schema
	 */
	public FeatureTypeProperty[] getProperties() {
		return properties[currentOutputFormatIndex];
	}
	
	/*
	 * Returns the properties for the currently set GML schema
	 */
	public List <FeatureTypeProperty> getPropertiesByType(PropertyType propertyType) {
		List <FeatureTypeProperty> foundProperties = 
			new ArrayList<FeatureTypeProperty>();
		for(p in properties[currentOutputFormatIndex]) {
			if(p.getPropertyType()==propertyType)
				foundProperties.add(p);
		}
		return foundProperties;
	}
	
	/*
	 * Returns the properties for the currently set GML schema
	 */
	public boolean hasPropertyType(PropertyType propertyType) {
		for(p in properties[currentOutputFormatIndex]) {
			if(p.getPropertyType()==propertyType)
				return true;
		}
		return false;
	}

	public void transferProperties() {
		ProjectHelper ph = new ProjectHelper();
		ph.setTransferProperty(
			"FT.NAME",
			this.prefixAndName
		);
		ph.setTransferProperty(
			"FT.PREFIX",
			this.qname.getPrefix()
		);
		ph.setTransferProperty(
			"FT.NAMESPACE", 
			this.qname.getNamespaceURI()
		);

		assert this.nsHolder[currentOutputFormatIndex] != null
		ph.setTransferProperty(
			"FT.NAMESPACES",
			this.nsHolder[currentOutputFormatIndex].getDeclarations()
		);
		ph.setTransferProperty(
			"FT.NAMESPACES.X",
			this.nsHolder[currentOutputFormatIndex].getDeclarationsForXExpressions()
		);
		ph.setTransferProperty(
			"FT.NAMESPACES.GET",
			this.nsHolder[currentOutputFormatIndex].getDeclarationsForGetMethod()
		);
		ph.setTransferProperty(
			"FT.NAMESPACES.GET.WFS2",
			this.nsHolder[currentOutputFormatIndex].getDeclarationsForGetMethodWfs2()
		);
		ph.setTransferProperty(
			"FT.OUTPUTFORMAT",
			this.outputFormats[currentOutputFormatIndex].getFormat()
		);
	}
	
	public NamespaceHolder getNamespaceHolder() {
		return this.nsHolder[currentOutputFormatIndex];
	}
	
	public void analyzeAndSetProperties(List<PropertySchemaAnalyzer> propertySchemaAnalyzer) {
		for(analyzer in propertySchemaAnalyzer) {
			outputFormats.add(analyzer.getOutputFormat());
			
			// TODO: Skip analysis on unknown outputFormat
			try {				
				List<FeatureTypeProperty> tmpProperties = analyzer.analyze(getQName());
				this.properties.add( tmpProperties.toArray(new FeatureTypeProperty[0]) );
				
				// Properties and FeatureType share the same NamespaceHolder
				NamespaceHolder h = this.properties[currentOutputFormatIndex][0].getNamespaceHolder();
				h.setNamespaceUriAndPrefix(qname.getNamespaceURI(), qname.getPrefix());
				this.nsHolder.add(h);
					
				for(p in this.properties[currentOutputFormatIndex]) {
					p.resolvePrefixes();
				}
			}catch(MaxDepthExceededException e) {
				throw new SchemaAnalysisException(analyzer, "Analysis of FeatureType "+this.getPrefixAndName()+
					" failed! "+e.getMessage());
			}
			currentOutputFormatIndex++;
		}
		Collections.sort(outputFormats);
	}
}
