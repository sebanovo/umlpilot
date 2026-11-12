package com.umlpilot.umlpilot_api.infrastructure.xmi;

import com.umlpilot.umlpilot_api.application.dto.XmiExportModel;
import com.umlpilot.umlpilot_api.application.port.out.XmiDiagramRenderer;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Serializa un diagrama UML a XMI 2.5.1 imitando la estructura que genera
 * Enterprise Architect (UMLDI + extension diagrams).
 */
@Component
public class EaXmiRenderer implements XmiDiagramRenderer {

    private static final String NS_XMI = "http://www.omg.org/spec/XMI/20131001";
    private static final String NS_UML = "http://www.omg.org/spec/UML/20131001";
    private static final String NS_UMLDI = "http://www.omg.org/spec/UML/20131001/UMLDI";
    private static final String NS_DC = "http://www.omg.org/spec/UML/20131001/UMLDC";
    private static final String NS_CUSTOM_PROFILE = "http://www.sparxsystems.com/profiles/thecustomprofile/1.0";
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private static final String STYLE1 = "ShowPrivate=1;ShowProtected=1;ShowPublic=1;HideRelationships=0;Locked=0;Border=1;HighlightForeign=1;PackageContents=1;SequenceNotes=0;ScalePrintImage=0;PPgs.cx=0;PPgs.cy=0;DocSize.cx=799;DocSize.cy=1067;ShowDetails=0;Orientation=P;Zoom=100;ShowTags=0;OpParams=1;VisibleAttributeDetail=0;ShowOpRetType=1;ShowIcons=1;CollabNums=0;HideProps=0;ShowReqs=0;ShowCons=0;PaperSize=1;HideParents=0;UseAlias=0;HideAtts=0;HideOps=0;HideStereo=0;HideElemStereo=0;ShowTests=0;ShowMaint=0;ConnectorNotation=UML 2.1;ExplicitNavigability=0;ShowShape=1;AllDockable=0;AdvancedElementProps=1;AdvancedFeatureProps=1;AdvancedConnectorProps=1;m_bElementClassifier=1;SPT=1;ShowNotes=0;SuppressBrackets=0;SuppConnectorLabels=0;PrintPageHeadFoot=0;ShowAsList=0;";
    private static final String STYLE2 = "ExcludeRTF=0;DocAll=0;HideQuals=0;AttPkg=1;ShowTests=0;ShowMaint=0;SuppressFOC=1;MatrixActive=0;SwimlanesActive=1;KanbanActive=0;MatrixLineWidth=1;MatrixLineClr=0;MatrixLocked=0;TConnectorNotation=UML 2.1;TExplicitNavigability=0;AdvancedElementProps=1;AdvancedFeatureProps=1;AdvancedConnectorProps=1;m_bElementClassifier=1;SPT=1;MDGDgm=;STBLDgm=;ShowNotes=0;VisibleAttributeDetail=0;ShowOpRetType=1;SuppressBrackets=0;SuppConnectorLabels=0;PrintPageHeadFoot=0;ShowAsList=0;SuppressedCompartments=;Theme=:119;";
    private static final String STYLE_SWIMLANES = "locked=false;orientation=0;width=0;inbar=false;names=false;color=-1;bold=false;fcol=0;tcol=-1;ofCol=-1;ufCol=-1;hl=0;ufh=0;hh=0;cls=0;bw=0;hli=0;SwimlaneFont=lfh:-13,lfw:0,lfi:0,lfu:0,lfs:0,lfface=Calibri,lfe:0,lfo:0,lfchar:1,lfop:0,lfcp:0,lfq:0,lfpf=0,lfWidth=0;";
    private static final String STYLE_MATRIX = "locked=false;matrixactive=false;swimlanesactive=true;kanbanactive=false;width=1;clrLine=0;";
    private static final String CONN_LABELS = "$LLB=CX=17:CY=14:OX=0:OY=0:HDN=0:BLD=0:ITA=0:UND=0:CLR=-1:ALN=1:DIR=0:ROT=0;LLT=;LMT=CX=32:CY=14:OX=0:OY=0:HDN=0:BLD=0:ITA=0:UND=0:CLR=-1:ALN=1:DIR=0:ROT=0;LMB=;LRT=;LRB=CX=17:CY=14:OX=0:OY=0:HDN=0:BLD=0:ITA=0:UND=0:CLR=-1:ALN=1:DIR=0:ROT=0;IRHS=;ILHS=;Path=;";

    private static final Set<String> MODELED_TYPES = Set.of(
            "association", "aggregation", "composition", "association_class", "generalization", "realization");

    @Override
    public byte[] render(XmiExportModel model) {
        List<String> out = new ArrayList<>();
        String diagramId = model.diagramId();
        String modelName = model.modelName();
        String pkgId = "EAPK_" + guid(diagramId);
        String diagramGuid = guid(diagramId + "_diagram");
        String diagramXmlId = "EAID_" + diagramGuid;

        List<XmiExportModel.Element> elements = model.elements();
        Map<String, XmiExportModel.Element> elementById = new LinkedHashMap<>();
        elements.forEach(e -> elementById.put(e.id(), e));

        List<XmiExportModel.Element> diagramNodes = elements.stream()
                .filter(e -> elementTag(e.elementType()) != null)
                .toList();
        Set<String> emitted = new LinkedHashSet<>(diagramNodes.stream().map(XmiExportModel.Element::id).toList());

        Map<String, XmiExportModel.Relationship> assocClassRel = new LinkedHashMap<>();
        for (XmiExportModel.Relationship rel : model.relationships()) {
            if ("association_class".equals(rel.type()) && rel.associationClassElementId() != null) {
                assocClassRel.put(rel.associationClassElementId(), rel);
            }
        }
        Set<String> assocClassIds = assocClassRel.keySet();

        Map<String, String> primitives = new LinkedHashMap<>();
        List<XmiExportModel.Relationship> relationships = model.relationships();
        Function<String, String> typeId = typeName -> resolveTypeId(typeName, elements, emitted, primitives);

        Map<String, List<XmiExportModel.Relationship>> generalizations = bySource(relationships, "generalization");
        Map<String, List<XmiExportModel.Relationship>> realizations = bySource(relationships, "realization");

        String[] diagramGuidParts = diagramGuid.split("_");

        out.add("<?xml version=\"1.0\" encoding=\"windows-1252\"?>");
        out.add("<xmi:XMI xmlns:uml=\"" + NS_UML + "\" xmlns:xmi=\"" + NS_XMI
                + "\" xmlns:umldi=\"" + NS_UMLDI + "\" xmlns:dc=\"" + NS_DC
                + "\" xmlns:thecustomprofile=\"" + NS_CUSTOM_PROFILE + "\">");
        out.add(p(1) + "<xmi:Documentation exporter=\"Enterprise Architect\" exporterVersion=\"6.5\"/>");
        out.add(p(1) + "<uml:Model xmi:type=\"uml:Model\" name=\"EA_Model\">");
        out.add(p(2) + "<packagedElement xmi:type=\"uml:Package\" xmi:id=\"" + pkgId + "\" name=\"" + esc(modelName) + "\">");

        for (XmiExportModel.Element element : elements) {
            String tag = elementTag(element.elementType());
            if (tag == null || assocClassIds.contains(element.id())) {
                continue;
            }
            out.add(p(3) + "<packagedElement xmi:type=\"" + tag + "\" xmi:id=\"" + xid(element.id())
                    + "\" name=\"" + esc(element.name()) + "\">");
            for (XmiExportModel.Relationship gen : generalizations.getOrDefault(element.id(), List.of())) {
                if (!validRel(gen, emitted)) continue;
                out.add(p(4) + "<generalization xmi:type=\"uml:Generalization\" xmi:id=\"" + xid(gen.id())
                        + "\" general=\"" + xid(gen.targetElementId()) + "\"/>");
            }
            for (XmiExportModel.Relationship real : realizations.getOrDefault(element.id(), List.of())) {
                if (!validRel(real, emitted)) continue;
                out.add(p(4) + "<interfaceRealization xmi:type=\"uml:InterfaceRealization\" xmi:id=\"" + xid(real.id())
                        + "\" name=\"" + esc(real.name()) + "\" client=\"" + xid(real.sourceElementId())
                        + "\" supplier=\"" + xid(real.targetElementId()) + "\" contract=\"" + xid(real.targetElementId()) + "\"/>");
            }
            emitAttributes(out, element.attributes(), 4, typeId);
            emitMethods(out, element.methods(), 4, typeId);
            out.add(p(3) + "</packagedElement>");
        }

        for (XmiExportModel.Relationship rel : relationships) {
            String type = rel.type();
            if (!"association".equals(type) && !"aggregation".equals(type)
                    && !"composition".equals(type) && !"association_class".equals(type)) {
                continue;
            }
            if (!validRel(rel, emitted)) continue;

            boolean isAssocClass = "association_class".equals(type);
            XmiExportModel.Element clsEl = isAssocClass ? elementById.get(rel.associationClassElementId()) : null;
            String relId = isAssocClass ? xid(clsEl != null ? clsEl.id() : rel.id()) : xid(rel.id());
            String src = xid(rel.id() + "_src");
            String dst = xid(rel.id() + "_dst");
            String tag = isAssocClass ? "uml:AssociationClass" : "uml:Association";
            String agg = "aggregation".equals(type) ? "shared" : "composition".equals(type) ? "composite" : "none";
            String name = isAssocClass ? (clsEl != null ? clsEl.name() : rel.name()) : rel.name();
            MultiplicityBounds sb = bounds(rel.sourceMultiplicity());
            MultiplicityBounds tb = bounds(rel.targetMultiplicity());

            out.add(p(3) + "<packagedElement xmi:type=\"" + tag + "\" xmi:id=\"" + relId + "\" name=\"" + esc(name) + "\">");
            out.add(p(4) + "<memberEnd xmi:idref=\"" + dst + "\"/>");
            out.add(p(4) + "<memberEnd xmi:idref=\"" + src + "\"/>");
            out.add(p(4) + "<ownedEnd xmi:type=\"uml:Property\" xmi:id=\"" + src + "\" association=\"" + relId + "\""
                    + (!"none".equals(agg) ? " aggregation=\"" + agg + "\"" : "") + ">");
            out.add(p(5) + "<type xmi:idref=\"" + xid(rel.sourceElementId()) + "\"/>");
            if (!isAssocClass && sb != null) {
                out.add(p(5) + "<lowerValue xmi:type=\"uml:LiteralInteger\" xmi:id=\"" + xid(rel.id() + "_slv") + "\" value=\"" + sb.lower() + "\"/>");
                out.add(p(5) + "<upperValue xmi:type=\"uml:LiteralUnlimitedNatural\" xmi:id=\"" + xid(rel.id() + "_suv") + "\" value=\"" + sb.upper() + "\"/>");
            }
            out.add(p(4) + "</ownedEnd>");
            out.add(p(4) + "<ownedEnd xmi:type=\"uml:Property\" xmi:id=\"" + dst + "\" association=\"" + relId + "\">");
            out.add(p(5) + "<type xmi:idref=\"" + xid(rel.targetElementId()) + "\"/>");
            if (!isAssocClass && tb != null) {
                out.add(p(5) + "<lowerValue xmi:type=\"uml:LiteralInteger\" xmi:id=\"" + xid(rel.id() + "_tlv") + "\" value=\"" + tb.lower() + "\"/>");
                out.add(p(5) + "<upperValue xmi:type=\"uml:LiteralUnlimitedNatural\" xmi:id=\"" + xid(rel.id() + "_tuv") + "\" value=\"" + tb.upper() + "\"/>");
            }
            out.add(p(4) + "</ownedEnd>");
            if (isAssocClass && clsEl != null) {
                emitAttributes(out, clsEl.attributes(), 4, typeId);
                emitMethods(out, clsEl.methods(), 4, typeId);
            }
            out.add(p(3) + "</packagedElement>");
        }

        out.add(p(2) + "</packagedElement>");

        out.add(p(2) + "<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\" xmi:id=\"" + diagramXmlId
                + "\" isFrame=\"false\" modelElement=\"" + pkgId + "\">");
        for (XmiExportModel.Element element : diagramNodes) {
            out.add(p(3) + "<ownedElement xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"" + shapeId(diagramGuidParts, element.id())
                    + "\" modelElement=\"" + xid(element.id()) + "\">");
            out.add(p(4) + "<ownedElement xmi:type=\"umldi:UMLNameLabel\" xmi:id=\"" + xid(element.id() + "_nl")
                    + "\" text=\"" + esc(element.name()) + "\"/>");
            out.add(p(4) + "<bounds xmi:type=\"dc:bounds\" xmi:id=\"" + xid(element.id() + "_db")
                    + "\" x=\"" + element.positionX() + "\" y=\"" + element.positionY()
                    + "\" width=\"" + element.width() + "\" height=\"" + element.height() + "\"/>");
            out.add(p(3) + "</ownedElement>");
        }
        for (XmiExportModel.Relationship rel : relationships) {
            if (!modeledRel(rel, emitted)) continue;
            XmiExportModel.Element source = elementById.get(rel.sourceElementId());
            XmiExportModel.Element target = elementById.get(rel.targetElementId());
            if (source == null || target == null) continue;
            out.add(p(3) + "<ownedElement xmi:type=\"umldi:UMLEdge\" xmi:id=\"" + shapeId(diagramGuidParts, rel.id())
                    + "\" source=\"" + shapeId(diagramGuidParts, source.id()) + "\" target=\"" + shapeId(diagramGuidParts, target.id())
                    + "\" modelElement=\"" + xid(rel.id()) + "\">");
            out.add(p(4) + "<waypoint xmi:type=\"dc:waypoint\" xmi:id=\"" + xid(rel.id() + "_wp0")
                    + "\" x=\"" + (source.positionX() + source.width() / 2) + "\" y=\"" + (source.positionY() + source.height() / 2) + "\"/>");
            out.add(p(4) + "<waypoint xmi:type=\"dc:waypoint\" xmi:id=\"" + xid(rel.id() + "_wp1")
                    + "\" x=\"" + (target.positionX() + target.width() / 2) + "\" y=\"" + (target.positionY() + target.height() / 2) + "\"/>");
            out.add(p(3) + "</ownedElement>");
        }
        out.add(p(2) + "</umldi:Diagram>");
        out.add(p(1) + "</uml:Model>");

        out.add(p(1) + "<xmi:Extension extender=\"Enterprise Architect\" extenderID=\"6.5\">");
        out.add(p(2) + "<elements>");
        int localId = 101;
        out.add(p(3) + "<element xmi:idref=\"" + pkgId + "\" xmi:type=\"uml:Package\" name=\"" + esc(modelName) + "\" scope=\"public\">");
        out.add(p(4) + "<model package2=\"" + xid(diagramId) + "\" package=\"" + pkgId + "\" tpos=\"0\" ea_localid=\"" + localId + "\" ea_eleType=\"package\"/>");
        out.add(p(4) + "<properties isSpecification=\"false\" sType=\"Package\" nType=\"0\" scope=\"public\"/>");
        out.add(p(4) + "<project author=\"UMLPilot\" version=\"1.0\" phase=\"1.0\" complexity=\"1\" status=\"Proposed\"/>");
        out.add(p(4) + "<code gentype=\"Java\"/>");
        out.add(p(4) + "<style appearance=\"BackColor=-1;BorderColor=-1;BorderWidth=-1;FontColor=-1;VSwimLanes=1;HSwimLanes=1;BorderStyle=0;\"/>");
        out.add(p(4) + "<tags/>");
        out.add(p(4) + "<xrefs/>");
        out.add(p(4) + "<extendedProperties tagged=\"0\" package_name=\"Model\"/>");
        out.add(p(4) + "<packageproperties version=\"1.0\"/>");
        out.add(p(4) + "<paths/>");
        out.add(p(4) + "<flags iscontrolled=\"FALSE\" isprotected=\"FALSE\" usedtd=\"FALSE\" logxml=\"FALSE\"/>");
        out.add(p(3) + "</element>");

        Map<String, Integer> localIds = new LinkedHashMap<>();
        for (XmiExportModel.Element element : elements) {
            String tag = elementTag(element.elementType());
            if (tag == null) continue;
            localId++;
            localIds.put(element.id(), localId);
            boolean isAssocClass = assocClassIds.contains(element.id());
            XmiExportModel.Relationship assocRel = assocClassRel.get(element.id());
            out.add(p(3) + "<element xmi:idref=\"" + xid(element.id()) + "\" xmi:type=\"" + tag
                    + "\" name=\"" + esc(element.name()) + "\" scope=\"public\">");
            out.add(p(4) + "<model package=\"" + pkgId + "\" tpos=\"0\" ea_localid=\"" + localId + "\" ea_eleType=\"element\"/>");
            out.add(p(4) + "<properties isSpecification=\"false\" sType=\"" + elementStype(element.elementType())
                    + "\" nType=\"" + (isAssocClass ? 17 : 0) + "\" scope=\"public\" isRoot=\"false\" isLeaf=\"false\" isAbstract=\"false\" isActive=\"false\"/>");
            out.add(p(4) + "<project author=\"UMLPilot\" version=\"1.0\" phase=\"1.0\" complexity=\"1\" status=\"Proposed\"/>");
            out.add(p(4) + "<code gentype=\"Java\"/>");
            out.add(p(4) + "<style appearance=\"BackColor=-1;BorderColor=-1;BorderWidth=-1;FontColor=-1;VSwimLanes=1;HSwimLanes=1;BorderStyle=0;\"/>");
            out.add(p(4) + "<tags/>");
            out.add(p(4) + "<xrefs/>");
            out.add(p(4) + "<extendedProperties tagged=\"0\" package_name=\"" + esc(modelName) + "\""
                    + (isAssocClass && assocRel != null ? " conID=\"" + xid(assocRel.id()) + "\"" : "") + "/>");

            if (!element.attributes().isEmpty()) {
                out.add(p(4) + "<attributes>");
                int index = 0;
                for (XmiExportModel.Attribute attr : element.attributes()) {
                    out.add(p(5) + "<attribute xmi:idref=\"" + xid(attr.id()) + "\" name=\"" + esc(attr.name())
                            + "\" scope=\"" + scope(attr.visibility()) + "\">");
                    out.add(p(6) + "<initial" + (attr.defaultValue() != null && !attr.defaultValue().isEmpty()
                            ? " value=\"" + esc(attr.defaultValue()) + "\"" : "") + "/>");
                    out.add(p(6) + "<documentation/>");
                    out.add(p(6) + "<properties type=\"" + esc(attr.dataType()) + "\" collection=\"false\" static=\""
                            + (attr.isStatic() ? "1" : "0") + "\" duplicates=\"0\" changeability=\"changeable\"/>");
                    out.add(p(6) + "<containment containment=\"Not Specified\" position=\"" + index + "\"/>");
                    out.add(p(6) + "<stereotype" + (attr.stereotype() != null ? " stereotype=\"" + esc(attr.stereotype()) + "\"" : "") + "/>");
                    out.add(p(6) + "<bounds lower=\"1\" upper=\"1\"/>");
                    out.add(p(5) + "</attribute>");
                    index++;
                }
                out.add(p(4) + "</attributes>");
            }

            if (!element.methods().isEmpty()) {
                out.add(p(4) + "<operations>");
                int index = 0;
                for (XmiExportModel.Method method : element.methods()) {
                    out.add(p(5) + "<operation xmi:idref=\"" + xid(method.id()) + "\" name=\"" + esc(method.name())
                            + "\" scope=\"" + scope(method.visibility()) + "\">");
                    out.add(p(6) + "<properties position=\"" + index + "\"/>");
                    out.add(p(6) + "<stereotype" + (method.stereotype() != null ? " stereotype=\"" + esc(method.stereotype()) + "\"" : "") + "/>");
                    out.add(p(6) + "<type type=\"" + esc(method.returnType() == null ? "void" : method.returnType())
                            + "\" const=\"false\" static=\"" + (method.isStatic() ? "true" : "false")
                            + "\" isAbstract=\"false\" synchronised=\"0\" concurrency=\"Sequential\" pure=\"0\" isQuery=\"false\"/>");
                    out.add(p(6) + "<parameters>");
                    out.add(p(7) + "<parameter xmi:idref=\"" + xid(method.id() + "_rt") + "\" visibility=\"public\">");
                    out.add(p(8) + "<properties pos=\"0\" type=\"" + esc(method.returnType() == null ? "void" : method.returnType()) + "\" const=\"false\"/>");
                    out.add(p(7) + "</parameter>");
                    for (XmiExportModel.Parameter param : method.parameters()) {
                        out.add(p(7) + "<parameter xmi:idref=\"" + xid(param.id()) + "\" visibility=\"public\">");
                        out.add(p(8) + "<properties pos=\"0\" type=\"" + esc(param.dataType()) + "\" const=\"false\"/>");
                        out.add(p(8) + "<documentation" + (param.notes() != null && !param.notes().isEmpty()
                                ? " value=\"" + esc(param.notes()) + "\"" : "") + "/>");
                        out.add(p(7) + "</parameter>");
                    }
                    out.add(p(6) + "</parameters>");
                    out.add(p(5) + "</operation>");
                    index++;
                }
                out.add(p(4) + "</operations>");
            }

            List<XmiExportModel.Relationship> links = relationships.stream()
                    .filter(r -> validRel(r, emitted)
                            && (r.sourceElementId().equals(element.id()) || r.targetElementId().equals(element.id())))
                    .toList();
            if (!links.isEmpty()) {
                out.add(p(4) + "<links>");
                for (XmiExportModel.Relationship link : links) {
                    String kind = "generalization".equals(link.type()) ? "Generalization" : "Association";
                    out.add(p(5) + "<" + kind + " xmi:id=\"" + xid(link.id()) + "\" start=\"" + xid(link.sourceElementId())
                            + "\" end=\"" + xid(link.targetElementId()) + "\"/>");
                }
                out.add(p(4) + "</links>");
            }
            out.add(p(3) + "</element>");
        }
        out.add(p(2) + "</elements>");

        out.add(p(2) + "<connectors>");
        for (XmiExportModel.Relationship rel : relationships) {
            if (!validRel(rel, emitted) || !MODELED_TYPES.contains(rel.type())) continue;
            boolean isGen = "generalization".equals(rel.type());
            boolean isReal = "realization".equals(rel.type());
            boolean isAssocClass = "association_class".equals(rel.type());
            String agg = "aggregation".equals(rel.type()) ? "shared" : "composition".equals(rel.type()) ? "composite" : "none";
            String sourceMultiplicity = rel.sourceMultiplicity() == null ? "" : rel.sourceMultiplicity().trim();
            String targetMultiplicity = rel.targetMultiplicity() == null ? "" : rel.targetMultiplicity().trim();
            XmiExportModel.Element source = elementById.get(rel.sourceElementId());
            XmiExportModel.Element target = elementById.get(rel.targetElementId());
            Integer sourceLocal = localIds.get(rel.sourceElementId());
            Integer targetLocal = localIds.get(rel.targetElementId());

            out.add(p(3) + "<connector xmi:idref=\"" + xid(rel.id()) + "\" name=\"" + esc(rel.name()) + "\">");
            out.add(p(4) + "<source xmi:idref=\"" + xid(rel.sourceElementId()) + "\">");
            out.add(p(5) + "<model" + (sourceLocal != null ? " ea_localid=\"" + sourceLocal + "\"" : "")
                    + " type=\"Class\" name=\"" + esc(source == null ? "" : source.name()) + "\"/>");
            out.add(p(5) + "<role visibility=\"Public\"/>");
            out.add(p(5) + "<type" + ((isGen || isReal) ? "" : (sourceMultiplicity.isEmpty() ? "" : " multiplicity=\"" + esc(sourceMultiplicity) + "\""))
                    + " aggregation=\"none\"/>");
            out.add(p(5) + "<constraints/>");
            out.add(p(5) + "<modifiers isOrdered=\"false\" isNavigable=\"false\"/>");
            out.add(p(5) + "<documentation/>");
            out.add(p(4) + "</source>");
            out.add(p(4) + "<target xmi:idref=\"" + xid(rel.targetElementId()) + "\">");
            out.add(p(5) + "<model" + (targetLocal != null ? " ea_localid=\"" + targetLocal + "\"" : "")
                    + " type=\"Class\" name=\"" + esc(target == null ? "" : target.name()) + "\"/>");
            out.add(p(5) + "<role visibility=\"Public\"/>");
            out.add(p(5) + "<type" + ((isGen || isReal) ? "" : (targetMultiplicity.isEmpty() ? "" : " multiplicity=\"" + esc(targetMultiplicity) + "\""))
                    + " aggregation=\"" + agg + "\"/>");
            out.add(p(5) + "<constraints/>");
            out.add(p(5) + "<modifiers isOrdered=\"false\" isNavigable=\"true\"/>");
            out.add(p(5) + "<documentation/>");
            out.add(p(4) + "</target>");
            String eaType = isGen ? "Generalization" : isReal ? "Realization" : "Association";
            out.add(p(4) + "<properties ea_type=\"" + eaType + "\"" + (isAssocClass ? " subtype=\"Class\"" : "")
                    + " direction=\"" + (isGen ? "Source -&gt; Destination" : "Unspecified") + "\"/>");
            out.add(p(4) + "<modifiers isRoot=\"false\" isLeaf=\"false\"/>");
            out.add(p(4) + "<appearance linemode=\"3\" linecolor=\"-1\" linewidth=\"0\" seqno=\"0\" headStyle=\"0\" lineStyle=\"0\"/>");
            if (isAssocClass || isGen || isReal) {
                out.add(p(4) + "<labels" + (rel.name() != null ? " mt=\"" + esc(rel.name()) + "\"" : "") + "/>");
            } else {
                List<String> parts = new ArrayList<>();
                if (!sourceMultiplicity.isEmpty()) parts.add("lb=\"" + esc(sourceMultiplicity) + "\"");
                if (rel.name() != null) parts.add("mt=\"" + esc(rel.name()) + "\"");
                if (!targetMultiplicity.isEmpty()) parts.add("rb=\"" + esc(targetMultiplicity) + "\"");
                out.add(parts.isEmpty() ? p(4) + "<labels/>" : p(4) + "<labels " + String.join(" ", parts) + "/>");
            }
            if (isAssocClass && rel.associationClassElementId() != null) {
                Integer local = localIds.get(rel.associationClassElementId());
                out.add(p(4) + "<extendedProperties virtualInheritance=\"0\" associationclass=\""
                        + xid(rel.associationClassElementId()) + "\"" + (local != null ? " privatedata1=\"" + local + "\"" : "") + "/>");
            } else {
                out.add(p(4) + "<extendedProperties virtualInheritance=\"0\"/>");
            }
            out.add(p(3) + "</connector>");
        }
        out.add(p(2) + "</connectors>");

        primitives.putIfAbsent("EAJava_void", "void");
        out.add(p(2) + "<primitivetypes>");
        out.add(p(3) + "<packagedElement xmi:type=\"uml:Package\" xmi:id=\"EAPrimitiveTypesPackage\" name=\"EA_PrimitiveTypes_Package\">");
        out.add(p(4) + "<packagedElement xmi:type=\"uml:Package\" xmi:id=\"EAJavaTypesPackage\" name=\"EA_Java_Types_Package\">");
        for (Map.Entry<String, String> primitive : primitives.entrySet()) {
            out.add(p(5) + "<packagedElement xmi:type=\"uml:PrimitiveType\" xmi:id=\"" + primitive.getKey()
                    + "\" name=\"" + esc(primitive.getValue()) + "\"/>");
        }
        out.add(p(4) + "</packagedElement>");
        out.add(p(3) + "</packagedElement>");
        out.add(p(2) + "</primitivetypes>");
        out.add(p(2) + "<profiles/>");

        out.add(p(2) + "<diagrams>");
        out.add(p(3) + "<diagram xmi:id=\"" + diagramXmlId + "\">");
        out.add(p(4) + "<model package=\"" + pkgId + "\" localID=\"1\" owner=\"" + pkgId + "\"/>");
        out.add(p(4) + "<properties name=\"" + esc(modelName) + "\" type=\"Logical\"/>");
        out.add(p(4) + "<project author=\"UMLPilot\" version=\"1.0\"/>");
        out.add(p(4) + "<style1 value=\"" + STYLE1 + "\"/>");
        out.add(p(4) + "<style2 value=\"" + STYLE2 + "\"/>");
        out.add(p(4) + "<swimlanes value=\"" + STYLE_SWIMLANES + "\"/>");
        out.add(p(4) + "<matrixitems value=\"" + STYLE_MATRIX + "\"/>");
        out.add(p(4) + "<extendedProperties/>");
        out.add(p(4) + "<elements>");
        int seqno = 1;
        for (XmiExportModel.Element element : diagramNodes) {
            out.add(p(5) + "<element geometry=\"Left=" + element.positionX() + ";Top=" + element.positionY()
                    + ";Right=" + (element.positionX() + element.width()) + ";Bottom=" + (element.positionY() + element.height())
                    + ";\" subject=\"" + xid(element.id()) + "\" seqno=\"" + seqno + "\" style=\"DUID=" + duid(element.id()) + ";\"/>");
            seqno++;
        }
        for (XmiExportModel.Relationship rel : relationships) {
            if (!modeledRel(rel, emitted)) continue;
            out.add(p(5) + "<element geometry=\"EDGE=2;SX=0;SY=0;EX=0;EY=0;" + CONN_LABELS + "\" subject=\"" + xid(rel.id())
                    + "\" style=\"Mode=3;EOID=" + duid(rel.targetElementId()) + ";SOID=" + duid(rel.sourceElementId())
                    + ";Color=-1;LWidth=0;Hidden=0;\"/>");
        }
        out.add(p(4) + "</elements>");
        out.add(p(3) + "</diagram>");
        out.add(p(2) + "</diagrams>");
        out.add(p(1) + "</xmi:Extension>");
        out.add("</xmi:XMI>");

        return String.join("\n", out).getBytes(WINDOWS_1252);
    }

    private void emitAttributes(List<String> out, List<XmiExportModel.Attribute> attributes, int indent,
                                Function<String, String> typeId) {
        for (XmiExportModel.Attribute attr : attributes) {
            out.add(p(indent) + "<ownedAttribute xmi:type=\"uml:Property\" xmi:id=\"" + xid(attr.id())
                    + "\" name=\"" + esc(attr.name()) + "\" visibility=\""
                    + ((attr.visibility() == null ? "private" : attr.visibility()).toLowerCase(Locale.ROOT)) + "\">");
            out.add(p(indent + 1) + "<lowerValue xmi:type=\"uml:LiteralInteger\" xmi:id=\"" + xid(attr.id() + "_lv") + "\" value=\"1\"/>");
            out.add(p(indent + 1) + "<upperValue xmi:type=\"uml:LiteralUnlimitedNatural\" xmi:id=\"" + xid(attr.id() + "_uv") + "\" value=\"1\"/>");
            out.add(p(indent + 1) + "<type xmi:idref=\"" + typeId.apply(attr.dataType()) + "\"/>");
            out.add(p(indent) + "</ownedAttribute>");
        }
    }

    private void emitMethods(List<String> out, List<XmiExportModel.Method> methods, int indent,
                             Function<String, String> typeId) {
        for (XmiExportModel.Method method : methods) {
            out.add(p(indent) + "<ownedOperation xmi:id=\"" + xid(method.id()) + "\" name=\"" + esc(method.name()) + "\">");
            for (XmiExportModel.Parameter param : method.parameters()) {
                out.add(p(indent + 1) + "<ownedParameter xmi:id=\"" + xid(param.id()) + "\" name=\""
                        + esc(param.name()) + "\" type=\"" + typeId.apply(param.dataType()) + "\"/>");
            }
            out.add(p(indent + 1) + "<ownedParameter xmi:id=\"" + xid(method.id() + "_rt") + "\" name=\"return\" direction=\"return\" type=\""
                    + typeId.apply(method.returnType()) + "\"/>");
            out.add(p(indent) + "</ownedOperation>");
        }
    }

    private static String resolveTypeId(String typeName, List<XmiExportModel.Element> elements,
                                        Set<String> emitted, Map<String, String> primitives) {
        String type = typeName == null || typeName.trim().isEmpty() ? "void" : typeName.trim();
        for (XmiExportModel.Element element : elements) {
            if (emitted.contains(element.id()) && type.equals(element.name())) {
                return xid(element.id());
            }
        }
        String clean = type.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (clean.isEmpty()) clean = "void";
        primitives.putIfAbsent("EAJava_" + clean, clean);
        return "EAJava_" + clean;
    }

    private static String shapeId(String[] diagramGuid, String id) {
        String[] elementGuid = guid(id).split("_");
        return "EAID_" + diagramGuid[0] + "_" + diagramGuid[1] + "_" + diagramGuid[2]
                + "_" + elementGuid[3] + "_" + elementGuid[4];
    }

    private static boolean validRel(XmiExportModel.Relationship rel, Set<String> emitted) {
        return emitted.contains(rel.sourceElementId()) && emitted.contains(rel.targetElementId());
    }

    private static boolean modeledRel(XmiExportModel.Relationship rel, Set<String> emitted) {
        return validRel(rel, emitted) && MODELED_TYPES.contains(rel.type());
    }

    private static Map<String, List<XmiExportModel.Relationship>> bySource(
            List<XmiExportModel.Relationship> relationships, String type) {
        Map<String, List<XmiExportModel.Relationship>> map = new LinkedHashMap<>();
        for (XmiExportModel.Relationship rel : relationships) {
            if (!type.equals(rel.type())) continue;
            map.computeIfAbsent(rel.sourceElementId(), key -> new ArrayList<>()).add(rel);
        }
        return map;
    }

    private static String elementTag(String type) {
        if (type == null) return null;
        return switch (type) {
            case "class" -> "uml:Class";
            case "interface" -> "uml:Interface";
            case "enumeration" -> "uml:Enumeration";
            case "dataType" -> "uml:DataType";
            default -> null;
        };
    }

    private static String elementStype(String type) {
        if (type == null) return "Class";
        return switch (type) {
            case "interface" -> "Interface";
            case "enumeration" -> "Enumeration";
            case "dataType" -> "DataType";
            default -> "Class";
        };
    }

    private static String scope(String visibility) {
        String value = visibility == null || visibility.isBlank() ? "public" : visibility.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String p(int count) {
        return "\t".repeat(count);
    }

    private static String xid(String raw) {
        return "EAID_" + guid(raw);
    }

    private static String duid(String raw) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < raw.length(); i++) {
            hash ^= raw.charAt(i);
            hash *= 16777619;
        }
        return String.format("%08X", hash);
    }

    private static int hash32(String value, int seed) {
        int hash = seed;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static String hex8(int value) {
        return String.format("%08X", value);
    }

    private static String guid(String raw) {
        String a = hex8(hash32(raw, 0x811c9dc5));
        String b = hex8(hash32(raw, 0x9e3779b9));
        String c = hex8(hash32(raw, 0x85ebca6b));
        String d = hex8(hash32(raw, 0xc2b2ae35));
        String e = hex8(hash32(raw, 0x27d4eb2f));
        String version4 = "4" + b.substring(5);
        int variantNibble = (Character.digit(c.charAt(0), 16) & 0x3) | 0x8;
        String variant = Integer.toHexString(variantNibble).toUpperCase(Locale.ROOT) + c.substring(1, 4);
        return a + "_" + b.substring(0, 4) + "_" + version4 + "_" + variant + "_" + (c.substring(4) + d + e).substring(0, 12);
    }

    private static MultiplicityBounds bounds(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.isEmpty()) return null;
        if (s.contains("..")) {
            String[] parts = s.split("\\.\\.", -1);
            String lower = parts.length > 0 && !parts[0].trim().isEmpty() ? parts[0].trim() : "0";
            String upper = parts.length > 1 && !parts[1].trim().isEmpty() ? parts[1].trim() : "*";
            return new MultiplicityBounds(lower, upper);
        }
        if ("*".equals(s)) return new MultiplicityBounds("0", "*");
        return new MultiplicityBounds(s, s);
    }

    private record MultiplicityBounds(String lower, String upper) {
    }
}
