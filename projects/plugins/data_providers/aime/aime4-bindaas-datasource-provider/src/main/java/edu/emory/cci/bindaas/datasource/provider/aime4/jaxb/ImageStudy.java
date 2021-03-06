//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.05.08 at 12:16:10 PM EDT 
//


package edu.emory.cci.bindaas.datasource.provider.aime4.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ImageStudy complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ImageStudy">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="instanceUid" type="{uri:iso.org:21090}II"/>
 *         &lt;element name="startDate" type="{uri:iso.org:21090}TS"/>
 *         &lt;element name="startTime" type="{uri:iso.org:21090}TS"/>
 *         &lt;element name="procedureDescription" type="{uri:iso.org:21090}ST" minOccurs="0"/>
 *         &lt;element name="imageSeries" type="{gme://caCORE.caCORE/4.4/edu.northwestern.radiology.AIM}ImageSeries"/>
 *         &lt;element name="referencedDicomObjectCollection" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="ReferencedDicomObject" type="{gme://caCORE.caCORE/4.4/edu.northwestern.radiology.AIM}ReferencedDicomObject" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ImageStudy", namespace = "gme://caCORE.caCORE/4.4/edu.northwestern.radiology.AIM", propOrder = {
    "instanceUid",
    "startDate",
    "startTime",
    "procedureDescription",
    "imageSeries",
    "referencedDicomObjectCollection"
})
public class ImageStudy {

    @XmlElement(required = true)
    protected II instanceUid;
    @XmlElement(required = true)
    protected TS startDate;
    @XmlElement(required = true)
    protected TS startTime;
    protected ST procedureDescription;
    @XmlElement(required = true)
    protected ImageSeries imageSeries;
    protected ImageStudy.ReferencedDicomObjectCollection referencedDicomObjectCollection;

    /**
     * Gets the value of the instanceUid property.
     * 
     * @return
     *     possible object is
     *     {@link II }
     *     
     */
    public II getInstanceUid() {
        return instanceUid;
    }

    /**
     * Sets the value of the instanceUid property.
     * 
     * @param value
     *     allowed object is
     *     {@link II }
     *     
     */
    public void setInstanceUid(II value) {
        this.instanceUid = value;
    }

    /**
     * Gets the value of the startDate property.
     * 
     * @return
     *     possible object is
     *     {@link TS }
     *     
     */
    public TS getStartDate() {
        return startDate;
    }

    /**
     * Sets the value of the startDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link TS }
     *     
     */
    public void setStartDate(TS value) {
        this.startDate = value;
    }

    /**
     * Gets the value of the startTime property.
     * 
     * @return
     *     possible object is
     *     {@link TS }
     *     
     */
    public TS getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link TS }
     *     
     */
    public void setStartTime(TS value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the procedureDescription property.
     * 
     * @return
     *     possible object is
     *     {@link ST }
     *     
     */
    public ST getProcedureDescription() {
        return procedureDescription;
    }

    /**
     * Sets the value of the procedureDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link ST }
     *     
     */
    public void setProcedureDescription(ST value) {
        this.procedureDescription = value;
    }

    /**
     * Gets the value of the imageSeries property.
     * 
     * @return
     *     possible object is
     *     {@link ImageSeries }
     *     
     */
    public ImageSeries getImageSeries() {
        return imageSeries;
    }

    /**
     * Sets the value of the imageSeries property.
     * 
     * @param value
     *     allowed object is
     *     {@link ImageSeries }
     *     
     */
    public void setImageSeries(ImageSeries value) {
        this.imageSeries = value;
    }

    /**
     * Gets the value of the referencedDicomObjectCollection property.
     * 
     * @return
     *     possible object is
     *     {@link ImageStudy.ReferencedDicomObjectCollection }
     *     
     */
    public ImageStudy.ReferencedDicomObjectCollection getReferencedDicomObjectCollection() {
        return referencedDicomObjectCollection;
    }

    /**
     * Sets the value of the referencedDicomObjectCollection property.
     * 
     * @param value
     *     allowed object is
     *     {@link ImageStudy.ReferencedDicomObjectCollection }
     *     
     */
    public void setReferencedDicomObjectCollection(ImageStudy.ReferencedDicomObjectCollection value) {
        this.referencedDicomObjectCollection = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="ReferencedDicomObject" type="{gme://caCORE.caCORE/4.4/edu.northwestern.radiology.AIM}ReferencedDicomObject" maxOccurs="unbounded"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "referencedDicomObject"
    })
    public static class ReferencedDicomObjectCollection {

        @XmlElement(name = "ReferencedDicomObject", namespace = "gme://caCORE.caCORE/4.4/edu.northwestern.radiology.AIM", required = true)
        protected List<ReferencedDicomObject> referencedDicomObject;

        /**
         * Gets the value of the referencedDicomObject property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the referencedDicomObject property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getReferencedDicomObject().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ReferencedDicomObject }
         * 
         * 
         */
        public List<ReferencedDicomObject> getReferencedDicomObject() {
            if (referencedDicomObject == null) {
                referencedDicomObject = new ArrayList<ReferencedDicomObject>();
            }
            return this.referencedDicomObject;
        }

    }

}
