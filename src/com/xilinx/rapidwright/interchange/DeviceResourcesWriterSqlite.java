/*
 * Copyright (c) 2020-2022, Xilinx, Inc.
 * Copyright (c) 2022-2023, Advanced Micro Devices, Inc.
 * All rights reserved.
 *
 * Author: Chris Lavin, Xilinx Research Labs.
 *
 * This file is part of RapidWright.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.xilinx.rapidwright.interchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.DesignTools;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.design.Unisim;
import com.xilinx.rapidwright.design.VivadoProp;
import com.xilinx.rapidwright.design.VivadoPropType;
import com.xilinx.rapidwright.device.BEL;
import com.xilinx.rapidwright.device.BELClass;
import com.xilinx.rapidwright.device.BELPin;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.FamilyType;
import com.xilinx.rapidwright.device.Grade;
import com.xilinx.rapidwright.device.IOBankType;
import com.xilinx.rapidwright.device.IOStandard;
import com.xilinx.rapidwright.device.IntentCode;
import com.xilinx.rapidwright.device.Node;
import com.xilinx.rapidwright.device.PIP;
import com.xilinx.rapidwright.device.PIPType;
import com.xilinx.rapidwright.device.Package;
import com.xilinx.rapidwright.device.PackagePin;
import com.xilinx.rapidwright.device.PseudoPIPHelper;
import com.xilinx.rapidwright.device.Series;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.SitePIP;
import com.xilinx.rapidwright.device.SitePIPStatus;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.Wire;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFLibrary;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.edif.EDIFTools;
import com.xilinx.rapidwright.interchange.DeviceResources.Device.WireCategory;
import com.xilinx.rapidwright.tests.CodePerfTracker;
import com.xilinx.rapidwright.util.Pair;

public class DeviceResourcesWriterSqlite {
    private static Connection connection;
    private static PreparedStatement ps_name_insert;
    private static PreparedStatement ps_siteTypeList_insert;
    private static PreparedStatement ps_bels_insert;
    private static PreparedStatement ps_belCategories_insert;
    private static PreparedStatement ps_belPins_insert;
    private static IdentityEnumerator<SiteTypeEnum> allSiteTypes;

    private static HashMap<TileTypeEnum,Tile> tileTypes;
    private static HashMap<SiteTypeEnum,Site> siteTypes;

    public static long rowid_insert_string(PreparedStatement ps, String str) {
        try {
            ps.setString(1, str);
            ResultSet rs1 = ps.executeQuery();
            long rowid = rs1.getLong(1);
            rs1.close();
            return rowid;
        }
        catch(SQLException e) {
          e.printStackTrace(System.err);
          return 0;
        }
    }

    public static long rowid_insert_string_string(PreparedStatement ps, String strA, String strB) {
        try {
            ps.setString(1, strA);
            ps.setString(2, strB);
            ResultSet rs1 = ps.executeQuery();
            long rowid = rs1.getLong(1);
            rs1.close();
            return rowid;
        }
        catch(SQLException e) {
          e.printStackTrace(System.err);
          return 0;
        }
    }

    public static long insert_device_name(String name) {
        return rowid_insert_string(ps_name_insert, name);
    }

    public static long insert_siteType(String name) {
        return rowid_insert_string(ps_siteTypeList_insert, name);
    }

    public static long insert_bel(long rowid_siteType, BEL bel) {
        try {
            ps_bels_insert.clearParameters();
            ps_bels_insert.setLong(1, rowid_siteType);
            ps_bels_insert.setString(2, bel.getName());
            ps_bels_insert.setString(3, bel.getBELType());
            BELClass category = bel.getBELClass();
            if (category == BELClass.BEL) ps_bels_insert.setString(4, "logic");
            if (category == BELClass.RBEL) ps_bels_insert.setString(4, "routing");
            if (category == BELClass.PORT) ps_bels_insert.setString(4, "sitePort");

            ResultSet rs1 = ps_bels_insert.executeQuery();
            long rowid = rs1.getLong(1);
            rs1.close();
            return rowid;
        }
        catch(SQLException e) {
          e.printStackTrace(System.err);
          return 0;
        }
    }

    public static long insert_belPin(long rowid_siteType, BELPin belPin) {
        if(belPin == null) return 0;
        try {
            ps_belPins_insert.clearParameters();
            ps_belPins_insert.setLong(1, rowid_siteType);
            ps_belPins_insert.setString(2, belPin.getName());
            BELPin.Direction dir = belPin.getDir();
            if (dir == BELPin.Direction.INPUT) ps_belPins_insert.setString(3, "input");
            if (dir == BELPin.Direction.OUTPUT) ps_belPins_insert.setString(3, "output");
            if (dir == BELPin.Direction.BIDIRECTIONAL) ps_belPins_insert.setString(3, "inout");
            ps_belPins_insert.setString(4, belPin.getBEL().getName());

            ResultSet rs1 = ps_belPins_insert.executeQuery();
            long rowid = rs1.getLong(1);
            rs1.close();
            return rowid;
        }
        catch(SQLException e) {
            e.printStackTrace(System.err);
            return 0;
        }
    }

    public static void populateSiteEnumerations(SiteInst siteInst, Site site) {
        if (!siteTypes.containsKey(siteInst.getSiteTypeEnum())) {
            if (site.getSiteTypeEnum() != siteInst.getSiteTypeEnum()) {
                return;
            }
            siteTypes.put(siteInst.getSiteTypeEnum(), site);
        }
    }

    public static void populateEnumerations(Design design, Device device) {

        allSiteTypes = new IdentityEnumerator<>();

        HashMap<SiteTypeEnum,Site> allAltSiteTypeEnums = new HashMap<>();

        tileTypes = new HashMap<>();
        siteTypes = new HashMap<>();
        for (Tile tile : device.getAllTiles()) {
            if (!tileTypes.containsKey(tile.getTileTypeEnum())) {
                tileTypes.put(tile.getTileTypeEnum(),tile);
            }
            for (Site site : tile.getSites()) {
                SiteInst siteInst = design.createSiteInst("site_instance", site.getSiteTypeEnum(), site);
                populateSiteEnumerations(siteInst, site);
                design.removeSiteInst(siteInst);

                SiteTypeEnum[] altSiteTypes = site.getAlternateSiteTypeEnums();
                for (int i=0; i < altSiteTypes.length; i++) {
                    SiteInst altSiteInst = design.createSiteInst("site_instance", altSiteTypes[i], site);
                    populateSiteEnumerations(altSiteInst, site);
                    design.removeSiteInst(altSiteInst);
                    if (!allAltSiteTypeEnums.containsKey(altSiteTypes[i])) {
                        allAltSiteTypeEnums.put(altSiteTypes[i], site);
                    }
                }
            }

        }
        Map<String, Pair<String, EnumSet<IOStandard>>> macroExpandExceptionMap =
                EDIFNetlist.macroExpandExceptionMap.getOrDefault(device.getSeries(), Collections.emptyMap());

        for (Entry<SiteTypeEnum, Site> altSiteType : allAltSiteTypeEnums.entrySet()) {
            if (!siteTypes.containsKey(altSiteType.getKey())) {
                siteTypes.put(altSiteType.getKey(), altSiteType.getValue());
            }
        }
    }
/*

    private static void writeCellParameterDefinitions(Series series, EDIFNetlist prims, ParameterDefinitions.Builder builder) {
        Set<String> cellsWithParameters = new HashSet<String>();
        for (EDIFLibrary library : prims.getLibraries()) {
            for (EDIFCell cell : library.getCells()) {
                String cellTypeName = cell.getName();

                Map<String,VivadoProp> defaultCellProperties = Design.getDefaultCellProperties(series, cellTypeName);
                if (defaultCellProperties != null && defaultCellProperties.size() > 0) {
                    cellsWithParameters.add(cellTypeName);
                }
            }
        }

        StructList.Builder<CellParameterDefinition.Builder> cellParamDefs = builder.initCells(cellsWithParameters.size());
        int i = 0;
        for (String cellTypeName : cellsWithParameters) {
            CellParameterDefinition.Builder cellParamDef = cellParamDefs.get(i);
            i += 1;


            cellParamDef.setCellType(allStrings.getIndex(cellTypeName));
            Map<String,VivadoProp> defaultCellProperties = Design.getDefaultCellProperties(series, cellTypeName);

            StructList.Builder<ParameterDefinition.Builder> paramDefs = cellParamDef.initParameters(defaultCellProperties.size());
            int j = 0;
            for (Map.Entry<String, VivadoProp> property : defaultCellProperties.entrySet()) {
                ParameterDefinition.Builder paramDef = paramDefs.get(j);
                j += 1;

                String propName = property.getKey();
                VivadoProp propValue = property.getValue();

                Integer nameIdx = allStrings.getIndex(propName);
                paramDef.setName(nameIdx);

                PropertyMap.Entry.Builder defaultValue = paramDef.getDefault();
                defaultValue.setKey(nameIdx);
                defaultValue.setTextValue(allStrings.getIndex(propValue.getValue()));

                if (propValue.getType() == VivadoPropType.BINARY) {
                    paramDef.setFormat(ParameterFormat.VERILOG_BINARY);
                } else if (propValue.getType() == VivadoPropType.BOOL) {
                    paramDef.setFormat(ParameterFormat.BOOLEAN);
                } else if (propValue.getType() == VivadoPropType.DOUBLE) {
                    paramDef.setFormat(ParameterFormat.FLOATING_POINT);
                } else if (propValue.getType() == VivadoPropType.HEX) {
                    paramDef.setFormat(ParameterFormat.VERILOG_HEX);
                } else if (propValue.getType() == VivadoPropType.INT) {
                    paramDef.setFormat(ParameterFormat.INTEGER);
                } else if (propValue.getType() == VivadoPropType.STRING) {
                    paramDef.setFormat(ParameterFormat.STRING);
                } else {
                    throw new RuntimeException(String.format("Unknown VivadoPropType %s", propValue.getType().name()));
                }
            }
        }
    }


    private static boolean containsUnusedMacros(EDIFCell cell, Set<EDIFCell> unusedMacros) {
        Queue<EDIFCell> q = new LinkedList<>();
        Set<EDIFCell> visited = new HashSet<>();
        q.add(cell);
        while (!q.isEmpty()) {
            EDIFCell curr = q.poll();
            visited.add(curr);
            if (unusedMacros.contains(curr)) {
                unusedMacros.add(curr);
                return true;
            }
            for (EDIFCellInst inst : cell.getCellInsts()) {
                EDIFCell child = inst.getCellType();
                if (visited.contains(child)) continue;
                q.add(child);
            }
        }
        return false;
    }

    public static void writeDeviceResourcesFile(String part, Device device, CodePerfTracker t,
            String fileName) throws IOException {
        writeDeviceResourcesFile(part, device, t, fileName, false);
    }
*/
    
    public static void writeDeviceResourcesFile(String part, Device device, CodePerfTracker t, 
            String fileName, boolean skipRouteResources) throws IOException {
        Design design = new Design();
        design.setPartName(part);
        Series series = device.getSeries();

        t.start("populateEnums");

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.execute("PRAGMA main.encoding = 'UTF-8';");
            statement.execute("PRAGMA main.foreign_keys = 1;");

            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/name/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/siteTypeList/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/belCategories/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/bels/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/belPins/create.sql")));

            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_BELClass/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_BELPin_Direction/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_FamilyType/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_IntentCode/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_IOBankType/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_IOStandard/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_PIPType/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_Series/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_SitePIPStatus/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_SiteTypeEnum/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_TileTypeEnum/create.sql")));
            statement.execute(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_WireCategory/create.sql")));

            ps_name_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/name/insert.sql")));
            ps_siteTypeList_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/siteTypeList/insert.sql")));
            ps_bels_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/bels/insert.sql")));
            ps_belCategories_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/belCategories/insert.sql")));
            ps_belPins_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/belPins/insert.sql")));

            rowid_insert_string(ps_belCategories_insert, "logic");
            rowid_insert_string(ps_belCategories_insert, "routing");
            rowid_insert_string(ps_belCategories_insert, "sitePort");

            PreparedStatement ps_enum_BELClass_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_BELClass/insert.sql")));
            PreparedStatement ps_enum_BELPin_Direction_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_BELPin_Direction/insert.sql")));
            PreparedStatement ps_enum_FamilyType_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_FamilyType/insert.sql")));
            PreparedStatement ps_enum_IntentCode_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_IntentCode/insert.sql")));
            PreparedStatement ps_enum_IOBankType_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_IOBankType/insert.sql")));
            PreparedStatement ps_enum_IOStandard_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_IOStandard/insert.sql")));
            PreparedStatement ps_enum_PIPType_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_PIPType/insert.sql")));
            PreparedStatement ps_enum_Series_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_Series/insert.sql")));
            PreparedStatement ps_enum_SitePIPStatus_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_SitePIPStatus/insert.sql")));
            PreparedStatement ps_enum_SiteTypeEnum_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_SiteTypeEnum/insert.sql")));
            PreparedStatement ps_enum_TileTypeEnum_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_TileTypeEnum/insert.sql")));
            PreparedStatement ps_enum_WireCategory_insert = connection.prepareStatement(Files.readString(Paths.get("interchange/fpga-sqlite-schema/DeviceResources/enum_WireCategory/insert.sql")));

            for (BELClass c : BELClass.values()) rowid_insert_string(ps_enum_BELClass_insert, c.toString());
            for (BELPin.Direction c : BELPin.Direction.values()) rowid_insert_string(ps_enum_BELPin_Direction_insert, c.toString());
            for (FamilyType c : FamilyType.values()) rowid_insert_string(ps_enum_FamilyType_insert, c.toString());
            for (IOBankType c: IOBankType.values()) rowid_insert_string(ps_enum_IOBankType_insert, c.toString());
            for (IOStandard c: IOStandard.values()) rowid_insert_string(ps_enum_IOStandard_insert, c.toString());
            for (PIPType c: PIPType.values()) rowid_insert_string(ps_enum_PIPType_insert, c.toString());
            for (Series c: Series.values()) rowid_insert_string(ps_enum_Series_insert, c.toString());
            for (SitePIPStatus c: SitePIPStatus.values()) rowid_insert_string(ps_enum_SitePIPStatus_insert, c.toString());
            for (SiteTypeEnum c: SiteTypeEnum.values()) rowid_insert_string(ps_enum_SiteTypeEnum_insert, c.toString());
            for (TileTypeEnum c: TileTypeEnum.values()) rowid_insert_string(ps_enum_TileTypeEnum_insert, c.toString());
            for (WireCategory c: WireCategory.values()) rowid_insert_string(ps_enum_WireCategory_insert, c.toString());
            for (IntentCode c: IntentCode.values()) rowid_insert_string_string(ps_enum_IntentCode_insert, c.toString(), WireType.intentToCategory(c).toString());

            connection.setAutoCommit(false);
            statement.execute("PRAGMA main.defer_foreign_keys = 1;");

            populateEnumerations(design, device);

            insert_device_name(device.getName());
            t.stop().start("SiteTypes");
            writeAllSiteTypesToBuilder(design, device);
    
            t.stop().start("TileTypes");

            /*
            
            Map<TileTypeEnum, Integer> tileTypeIndicies = writeAllTileTypesToBuilder(design, device, devBuilder);
            Map<TileTypeEnum, TileType.Builder> tileTypesObj = new HashMap<TileTypeEnum, TileType.Builder>();
            for (Map.Entry<TileTypeEnum, Integer> tileType : tileTypeIndicies.entrySet()) {
                tileTypesObj.put(tileType.getKey(), devBuilder.getTileTypeList().get(tileType.getValue()));
            }
            */
            t.stop().start("Tiles");
            /*
            writeAllTilesToBuilder(device, devBuilder, tileTypeIndicies);

            */
            t.stop().start("Wires&Nodes");
            /*
            writeAllWiresAndNodesToBuilder(device, devBuilder, skipRouteResources);
            */

            t.stop().start("Prims&Macros");
            /*
            // Create an EDIFNetlist populated with just primitive and macro libraries
            EDIFLibrary prims = Design.getPrimitivesLibrary(device.getName());
            EDIFLibrary macros = Design.getMacroPrimitives(series);
            Set<EDIFCell> unsupportedMacros = new HashSet<>();
            EDIFNetlist netlist = new EDIFNetlist("PrimitiveLibs");
            netlist.addLibrary(prims);
            netlist.addLibrary(macros);
            List<EDIFCell> dupsToRemove = new ArrayList<EDIFCell>();
            for (EDIFCell hdiCell : prims.getCells()) {
                EDIFCell cell = macros.getCell(hdiCell.getName());
                if (cell != null) {
                    dupsToRemove.add(hdiCell);
                }
            }

            for (EDIFCell dupCell : dupsToRemove) {
                prims.removeCell(dupCell);
            }

            for (EDIFCell cell : macros.getCells()) {
                for (EDIFCellInst inst : cell.getCellInsts()) {
                    EDIFCell instCell = inst.getCellType();
                    if (!prims.containsCell(instCell) && !macros.containsCell(instCell)) {
                        unsupportedMacros.add(cell);
                        continue;
                    }
                    EDIFCell macroCell = macros.getCell(instCell.getName());
                    if (macroCell != null && !unsupportedMacros.contains(macroCell)) {
                        // remap cell definition to macro library
                        inst.setCellType(macroCell);
                    }
                }
            }

            // Not all devices have all the primitives to support all macros, thus we will remove
            // them to avoid stale references
            for (EDIFCell macro : new ArrayList<>(macros.getCells())) {
                if (containsUnusedMacros(macro, unsupportedMacros)) {
                    macros.removeCell(macro);
                }
            }

            Map<String, Pair<String, EnumSet<IOStandard>>> macroCollapseExceptionMap =
                    EDIFNetlist.macroCollapseExceptionMap.getOrDefault(series, Collections.emptyMap());
            List<Unisim> unisims = new ArrayList<Unisim>();
            for (EDIFCell cell : macros.getCells()) {
                String cellName = cell.getName();
                Pair<String, EnumSet<IOStandard>> entry = macroCollapseExceptionMap.get(cellName);
                if (entry != null) {
                    cellName = entry.getFirst();
                }
                Unisim unisim = Unisim.valueOf(cellName);
                Map<String,String> invertiblePins = DesignTools.getInvertiblePinMap(series, unisim);
                if (invertiblePins != null && invertiblePins.size() > 0) {
                    unisims.add(unisim);
                }
            }
            for (EDIFCell cell : prims.getCells()) {
                Unisim unisim = Unisim.valueOf(cell.getName());
                Map<String,String> invertiblePins = DesignTools.getInvertiblePinMap(series, unisim);
                if (invertiblePins != null && invertiblePins.size() > 0) {
                    unisims.add(unisim);
                }
            }

            StructList.Builder<CellInversion.Builder> cellInversions = devBuilder.initCellInversions(unisims.size());
            for (int i = 0; i < unisims.size(); ++i) {
                Unisim unisim = unisims.get(i);
                CellInversion.Builder cellInversion = cellInversions.get(i);
                cellInversion.setCell(allStrings.getIndex(unisim.name()));

                Map<String,String> invertiblePins = DesignTools.getInvertiblePinMap(series, unisim);
                StructList.Builder<CellPinInversion.Builder> cellPinInversions = cellInversion.initCellPins(invertiblePins.size());

                int j = 0;
                for (Map.Entry<String, String> entry : invertiblePins.entrySet()) {
                    String port = entry.getKey();
                    String parameterStr = entry.getValue();

                    CellPinInversion.Builder pinInversion = cellPinInversions.get(j);
                    j += 1;

                    pinInversion.setCellPin(allStrings.getIndex(port));

                    CellPinInversionParameter.Builder param = pinInversion.getNotInverting();
                    PropertyMap.Entry.Builder parameter = param.initParameter();
                    parameter.setKey(allStrings.getIndex(parameterStr));
                    parameter.setTextValue(allStrings.getIndex("1'b0"));

                    param = pinInversion.getInverting();
                    parameter = param.initParameter();
                    parameter.setKey(allStrings.getIndex(parameterStr));
                    parameter.setTextValue(allStrings.getIndex("1'b1"));
                }
            }

            Netlist.Builder netlistBuilder = devBuilder.getPrimLibs();
            netlistBuilder.setName(netlist.getName());
            LogNetlistWriter writer = new LogNetlistWriter(allStrings, new HashMap<String, String>() {{
                        put(EDIFTools.EDIF_LIBRARY_HDI_PRIMITIVES_NAME, LogNetlistWriter.DEVICE_PRIMITIVES_LIB);
                        put(series+"_"+EDIFTools.MACRO_PRIMITIVES_LIB, LogNetlistWriter.DEVICE_MACROS_LIB);
                    }}
                );
            writer.populateNetlistBuilder(netlist, netlistBuilder, CodePerfTracker.SILENT);

            writeCellParameterDefinitions(series, netlist, devBuilder.getParameterDefs());

            // Write macro exception map
            Map<String, Pair<String, EnumSet<IOStandard>>> expandMap =
                    EDIFNetlist.macroExpandExceptionMap.getOrDefault(series, Collections.emptyMap());
            Map<String, MacroParamRule[]> paramRules = MacroParamMappingRules.macroRules.get(series);
            Set<String> exceptionMacros = new TreeSet<>(expandMap.keySet());
            exceptionMacros.addAll(paramRules.keySet());
            int size = exceptionMacros.size();
            StructList.Builder<PrimToMacroExpansion.Builder> exceptionMap =
                    devBuilder.initExceptionMap(size);
            int i=0;
            int ioStdPropIdx = allStrings.getIndex(EDIFNetlist.IOSTANDARD_PROP);
            for (String macroName : exceptionMacros) {
                PrimToMacroExpansion.Builder entryBuilder = exceptionMap.get(i);
                entryBuilder.setPrimName(allStrings.getIndex(macroName));
                entryBuilder.setMacroName(allStrings.getIndex(macroName));

                // Check if this macro has an expansion exception
                if (expandMap.containsKey(macroName)) {
                    Pair<String, EnumSet<IOStandard>> expandException = expandMap.get(macroName);
                    entryBuilder.setMacroName(allStrings.getIndex(expandException.getFirst()));

                    StructList.Builder<PropertyMap.Entry.Builder> ioStdEntries =
                            entryBuilder.initParameters(expandException.getSecond().size());
                    int j=0;
                    for (IOStandard ioStd : expandException.getSecond()) {
                        PropertyMap.Entry.Builder ioStdEntry = ioStdEntries.get(j);
                        ioStdEntry.setKey(ioStdPropIdx);
                        ioStdEntry.setTextValue(allStrings.getIndex(ioStd.name()));
                        j++;
                    }
                }

                // Check if this macro has a parameter propagation rule set
                if (paramRules.containsKey(macroName)) {
                    MacroParamRule[] rules = paramRules.get(macroName);
                    StructList.Builder<ParameterMapRule.Builder> parameterMap =
                            entryBuilder.initParamMapping(rules.length);
                    int j=0;
                    for (MacroParamRule rule : rules) {
                        ParameterMapRule.Builder ruleBuilder = parameterMap.get(j);
                        ruleBuilder.setPrimParam(allStrings.getIndex(rule.getPrimParam()));
                        ruleBuilder.setInstName(allStrings.getIndex(rule.getInstName()));
                        ruleBuilder.setInstParam(allStrings.getIndex(rule.getInstParam()));
                        if (rule.getBitSlice() != null) {
                            PrimitiveList.Int.Builder bitsBuilder =
                                    ruleBuilder.initBitSlice(rule.getBitSlice().length);
                            for (int k = 0; k < rule.getBitSlice().length; k++) {
                                bitsBuilder.set(k, rule.getBitSlice()[k]);
                            }
                        } else if (rule.getTableLookup() != null) {
                            // Lookup table
                            StructList.Builder<ParameterMapEntry.Builder> tableBuilder =
                                ruleBuilder.initTableLookup(rule.getTableLookup().length);
                            for (int k = 0; k < rule.getTableLookup().length; k++) {
                                ParameterMapEntry.Builder itemBuilder = tableBuilder.get(k);
                                MacroParamTableEntry tableEntry = rule.getTableLookup()[k];
                                itemBuilder.setFrom(allStrings.getIndex(tableEntry.from));
                                itemBuilder.setFrom(allStrings.getIndex(tableEntry.to));
                            }
                        } else {
                            ruleBuilder.setCopyValue(Void.VOID);
                        }
                        j++;
                    }
                }
                i++;
            }
            */
            t.stop().start("Cell <-> BEL pin map");
            /*
            EnumerateCellBelMapping.populateAllPinMappings(part, device, devBuilder, allStrings);

            */
            t.stop().start("Packages");
            /*
            populatePackages(allStrings, device, devBuilder);

            */
            t.stop().start("Constants");
            /*
            ConstantDefinitions.writeConstants(allStrings, device, devBuilder.initConstants(), design, siteTypes, tileTypesObj);

            */
            t.stop().start("Write File");

            connection.commit();
            connection.setAutoCommit(true);
            statement.executeUpdate("backup to " + fileName);
            connection.close();

            t.stop();

        } catch(SQLException e) {
        e.printStackTrace(System.err);
        }

    }

    public static void writeAllSiteTypesToBuilder(Design design, Device device) {
        for (Entry<SiteTypeEnum,Site> e : siteTypes.entrySet()) {
            Site site = e.getValue();
            SiteInst siteInst = design.createSiteInst("site_instance", e.getKey(), site);
            Tile tile = siteInst.getTile();
            long rowid_siteType = insert_siteType(e.getKey().name());
            allSiteTypes.addObject(e.getKey());

            IdentityEnumerator<BELPin> allBELPins = new IdentityEnumerator<BELPin>();
            
            // BELs
            for (int j=0; j < siteInst.getBELs().length; j++) {
                BEL bel = siteInst.getBELs()[j];
                insert_bel(rowid_siteType, bel);
                // PrimitiveList.Int.Builder belPinsBuilder = belBuilder.initPins(bel.getPins().length);
                for (int k=0; k < bel.getPins().length; k++) {
                    BELPin belPin = bel.getPin(k);
                    allBELPins.addObject(belPin);
                }
                // if (bel.canInvert()) {
                //     BELInverter.Builder belInverter = belBuilder.initInverting();
                allBELPins.addObject(bel.getNonInvertingPin());
                allBELPins.addObject(bel.getInvertingPin());
                // } else {
                //     belBuilder.setNonInverting(Void.VOID);
                // }
            }
            
            // SitePins
            int highestIndexInputPin = siteInst.getHighestSitePinInputIndex();
            ArrayList<String> pinNames = new ArrayList<String>();
            for (String pinName : siteInst.getSitePinNames()) {
                pinNames.add(pinName);
            }
            // siteType.setLastInput(highestIndexInputPin);

            // StructList.Builder<SitePin.Builder> pins = siteType.initPins(pinNames.size());
            for (int j=0; j < pinNames.size(); j++) {
                String primarySitePinName = pinNames.get(j);
                int sitePinIndex = site.getPinIndex(pinNames.get(j));
                if (sitePinIndex == -1) {
                    primarySitePinName = siteInst.getPrimarySitePinName(pinNames.get(j));
                    sitePinIndex = site.getPinIndex(primarySitePinName);
                }

                if (sitePinIndex == -1) {
                    throw new RuntimeException("Failed to find pin index for site " + site.getName() + " site type " + e.getKey().name()+ " site pin " + primarySitePinName + " / " + pinNames.get(j));
                }

                // SitePin.Builder pin = pins.get(j);
                // pin.setName(allStrings.getIndex(pinNames.get(j)));
                // pin.setDir(j <= highestIndexInputPin ? Direction.INPUT : Direction.OUTPUT);
                BEL bel = siteInst.getBEL(pinNames.get(j));
                BELPin[] belPins = bel.getPins();
                if (belPins.length != 1) {
                    throw new RuntimeException("Only expected 1 BEL pin on site pin BEL.");
                }
                BELPin belPin = belPins[0];
                allBELPins.addObject(belPin);
            }

            // SiteWires
            String[] siteWires = siteInst.getSiteWires();
            // StructList.Builder<SiteWire.Builder> swBuilders =
            //         siteType.initSiteWires(siteWires.length);
            for (int j=0; j < siteWires.length; j++) {
                // SiteWire.Builder swBuilder = swBuilders.get(j);
                String siteWireName = siteWires[j];
                // swBuilder.setName(allStrings.getIndex(siteWireName));
                BELPin[] swPins = siteInst.getSiteWirePins(siteWireName);
                // PrimitiveList.Int.Builder bpBuilders = swBuilder.initPins(swPins.length);
                for (int k=0; k < swPins.length; k++) {
                    allBELPins.addObject(swPins[k]);
                }
            }
            
            // Write out BEL pins.
            for (int j=0; j < allBELPins.size(); j++) {
                insert_belPin(rowid_siteType, allBELPins.get(j));
            }
            /*
            SitePIP[] allSitePIPs = siteInst.getSitePIPs();

            // Write out SitePIPs
            StructList.Builder<DeviceResources.Device.SitePIP.Builder> spBuilders =
                    siteType.initSitePIPs(allSitePIPs.length);
            for (int j=0; j < allSitePIPs.length; j++) {
                DeviceResources.Device.SitePIP.Builder spBuilder = spBuilders.get(j);
                SitePIP sitePIP = allSitePIPs[j];
                spBuilder.setInpin(allBELPins.getIndex(sitePIP.getInputPin()));
                spBuilder.setOutpin(allBELPins.getIndex(sitePIP.getOutputPin()));
            }
*/

            design.removeSiteInst(siteInst);
        }

        for (Entry<SiteTypeEnum,Site> e : siteTypes.entrySet()) {
/*
            Site site = e.getValue();

            SiteType.Builder siteType = siteTypesList.get(i);

            SiteTypeEnum[] altSiteTypes = site.getAlternateSiteTypeEnums();
            PrimitiveList.Int.Builder altSiteTypesBuilder = siteType.initAltSiteTypes(altSiteTypes.length);

            for (int j=0; j < altSiteTypes.length; ++j) {
                Integer siteTypeIdx = allSiteTypes.maybeGetIndex(altSiteTypes[j]);
                if (siteTypeIdx == null) {
                    throw new RuntimeException("Site type " + altSiteTypes[j].name() + " is missing from allSiteTypes Enumerator.");
                }
                altSiteTypesBuilder.set(j, siteTypeIdx);
            }

            */
        }
    }
/*
    private static void populateAltSitePins(
            Design design,
            Site site,
            int primaryTypeIndex,
            StructList.Builder<DeviceResources.Device.ParentPins.Builder> listOfParentPins,
            DeviceResources.Device.Builder devBuilder) {
        PrimitiveList.Int.Builder altSiteTypes = devBuilder.getSiteTypeList().get(primaryTypeIndex).getAltSiteTypes();
        SiteTypeEnum[] altSiteTypeEnums = site.getAlternateSiteTypeEnums();
        for (int i = 0; i < altSiteTypeEnums.length; ++i) {
            SiteInst siteInst = design.createSiteInst("site_instance", altSiteTypeEnums[i], site);

            DeviceResources.Device.SiteType.Builder altSiteType = devBuilder.getSiteTypeList().get(altSiteTypes.get(i));
            StructList.Builder<DeviceResources.Device.SitePin.Builder> sitePins = altSiteType.getPins();
            PrimitiveList.Int.Builder parentPins = listOfParentPins.get(i).initPins(altSiteType.getPins().size());

            for (int j = 0; j < sitePins.size(); j++) {
                DeviceResources.Device.SitePin.Builder sitePin = sitePins.get(j);
                String sitePinName = allStrings.get(sitePin.getName());
                String parentPinName = siteInst.getPrimarySitePinName(sitePinName);
                parentPins.set(j, site.getPinIndex(parentPinName));
            }

            design.removeSiteInst(siteInst);
        }
    }

    public static Map<TileTypeEnum, Integer> writeAllTileTypesToBuilder(Design design, Device device, DeviceResources.Device.Builder devBuilder) {
        StructList.Builder<TileType.Builder> tileTypesList = devBuilder.initTileTypeList(tileTypes.size());

        Map<TileTypeEnum, Integer> tileTypeIndicies = new HashMap<TileTypeEnum, Integer>();

        int i=0;
        for (Entry<TileTypeEnum,Tile> e : tileTypes.entrySet()) {
            Tile tile = e.getValue();
            TileType.Builder tileType = tileTypesList.get(i);
            tileTypeIndicies.put(e.getKey(), i);
            // name
            tileType.setName(allStrings.getIndex(e.getKey().name()));

            // siteTypes
            Site[] sites = tile.getSites();
            StructList.Builder<DeviceResources.Device.SiteTypeInTileType.Builder> siteTypes = tileType.initSiteTypes(sites.length);
            for (int j=0; j < sites.length; j++) {
                DeviceResources.Device.SiteTypeInTileType.Builder siteType = siteTypes.get(j);
                int primaryTypeIndex = allSiteTypes.getIndex(sites[j].getSiteTypeEnum());
                siteType.setPrimaryType(primaryTypeIndex);

                int numPins = sites[j].getSitePinCount();
                PrimitiveList.Int.Builder pinWires = siteType.initPrimaryPinsToTileWires(numPins);
                for (int k=0; k < numPins; ++k) {
                    pinWires.set(k, allStrings.getIndex(sites[j].getTileWireNameFromPinName(sites[j].getPinName(k))));
                }

                populateAltSitePins(
                        design,
                        sites[j],
                        primaryTypeIndex,
                        siteType.initAltPinsToPrimaryPins(sites[j].getAlternateSiteTypeEnums().length),
                        devBuilder);
            }

            // wires
            PrimitiveList.Int.Builder wires = tileType.initWires(tile.getWireCount());
            for (int j=0 ; j < tile.getWireCount(); j++) {
                wires.set(j, allStrings.getIndex(tile.getWireName(j)));
            }

            // pips
            ArrayList<PIP> pips = tile.getPIPs();
            StructList.Builder<DeviceResources.Device.PIP.Builder> pipBuilders =
                    tileType.initPips(pips.size());
            for (int j=0; j < pips.size(); j++) {
                DeviceResources.Device.PIP.Builder pipBuilder = pipBuilders.get(j);
                PIP pip = pips.get(j);
                pipBuilder.setWire0(pip.getStartWireIndex());
                pipBuilder.setWire1(pip.getEndWireIndex());
                pipBuilder.setDirectional(!pip.isBidirectional());
                if (pip.getPIPType() == PIPType.BI_DIRECTIONAL_BUFFERED20) {
                    pipBuilder.setBuffered20(true);
                } else if (pip.getPIPType() == PIPType.BI_DIRECTIONAL_BUFFERED21_BUFFERED20) {
                    pipBuilder.setBuffered20(true);
                    pipBuilder.setBuffered21(true);
                } else if (pip.getPIPType() == PIPType.DIRECTIONAL_BUFFERED21) {
                    pipBuilder.setBuffered21(true);
                }

                if (pip.isRouteThru()) {
                    PseudoPIPHelper pseudoPIPHelper = PseudoPIPHelper.getPseudoPIPHelper(pip);
                    List<BELPin> belPins = pseudoPIPHelper.getUsedBELPins();
                    if (belPins == null || belPins.size() < 1) continue;

                    HashMap<BEL,ArrayList<BELPin>> pins = new HashMap<BEL, ArrayList<BELPin>>();
                    for (BELPin pin : belPins) {
                        ArrayList<BELPin> currBELPins = pins.get(pin.getBEL());
                        if (currBELPins == null) {
                            currBELPins = new ArrayList<>();
                            pins.put(pin.getBEL(), currBELPins);
                        }
                        currBELPins.add(pin);
                    }
                    StructList.Builder<PseudoCell.Builder> pseudoCells = pipBuilder.initPseudoCells(pins.size());
                    int k=0;
                    for (Entry<BEL, ArrayList<BELPin>> e3 : pins.entrySet()) {
                        PseudoCell.Builder pseudoCell = pseudoCells.get(k);
                        pseudoCell.setBel(allStrings.getIndex(e3.getKey().getName()));
                        List<BELPin> usedPins = e3.getValue();
                        int pinCount = usedPins.size();
                        Int.Builder pinsBuilder = pseudoCell.initPins(pinCount);
                        for (int l=0; l < pinCount; l++) {
                            pinsBuilder.set(l, allStrings.getIndex(usedPins.get(l).getName()));
                        }
                        k++;
                    }
                }
            }
            i++;
        }

        return tileTypeIndicies;
    }

    public static void writeAllTilesToBuilder(Device device, DeviceResources.Device.Builder devBuilder, Map<TileTypeEnum, Integer> tileTypeIndicies) {
        Collection<Tile> tiles = device.getAllTiles();
        StructList.Builder<DeviceResources.Device.Tile.Builder> tileBuilders =
                devBuilder.initTileList(tiles.size());

        int i=0;
        for (Tile tile : tiles) {
            DeviceResources.Device.Tile.Builder tileBuilder = tileBuilders.get(i);
            tileBuilder.setName(allStrings.getIndex(tile.getName()));
            tileBuilder.setType(tileTypeIndicies.get(tile.getTileTypeEnum()));
            Site[] sites = tile.getSites();
            StructList.Builder<DeviceResources.Device.Site.Builder> siteBuilders =
                    tileBuilder.initSites(sites.length);
            for (int j=0; j < sites.length; j++) {
                DeviceResources.Device.Site.Builder siteBuilder = siteBuilders.get(j);
                siteBuilder.setName(allStrings.getIndex(sites[j].getName()));
                siteBuilder.setType(j);
            }
            tileBuilder.setRow((short)tile.getRow());
            tileBuilder.setCol((short)tile.getColumn());
            i++;
        }

    }

    private static long makeKey(Tile tile, int wire) {
        long key = wire;
        key = (((long)tile.getUniqueAddress()) << 32) | key;
        return key;
    }

    public static void writeAllWiresAndNodesToBuilder(Device device, DeviceResources.Device.Builder devBuilder,
            boolean skipRouteResources) {
        LongEnumerator allWires = new LongEnumerator();
        ArrayList<Long> allNodes = new ArrayList<>();

        if (!skipRouteResources) {
            for (Tile tile : device.getAllTiles()) {
                for (int i = 0; i < tile.getWireCount(); i++) {
                    Wire wire = new Wire(tile, i);
                    allWires.addObject(makeKey(wire.getTile(), wire.getWireIndex()));

                    Node node = wire.getNode();
                    if (node == null)
                        continue;
                    if (node.getTile() == tile && node.getWire() == i)
                        allNodes.add(makeKey(node.getTile(), node.getWire()));
                }
            }
        }

        StructList.Builder<DeviceResources.Device.Wire.Builder> wireBuilders =
                devBuilder.initWires(allWires.size());

        for (int i=0; i < allWires.size(); i++) {
            DeviceResources.Device.Wire.Builder wireBuilder = wireBuilders.get(i);
            long wireKey = allWires.get(i);
            Wire wire = new Wire(device.getTile((int)(wireKey >>> 32)), (int)(wireKey & 0xffffffff));
            //Wire wire = allWires.get(i);
            wireBuilder.setTile(allStrings.getIndex(wire.getTile().getName()));
            wireBuilder.setWire(allStrings.getIndex(wire.getWireName()));
            wireBuilder.setType(wire.getIntentCode().ordinal());
        }

        StructList.Builder<DeviceResources.Device.Node.Builder> nodeBuilders =
                devBuilder.initNodes(allNodes.size());
        for (int i=0; i < allNodes.size(); i++) {
            DeviceResources.Device.Node.Builder nodeBuilder = nodeBuilders.get(i);
            //Node node = allNodes.get(i);
            long nodeKey = allNodes.get(i);
            Node node = Node.getNode(device.getTile((int)(nodeKey >>> 32)), (int)(nodeKey & 0xffffffff));
            Wire[] wires = node.getAllWiresInNode();
            PrimitiveList.Int.Builder wBuilders = nodeBuilder.initWires(wires.length);
            for (int k=0; k < wires.length; k++) {
                wBuilders.set(k, allWires.getIndex(makeKey(wires[k].getTile(), wires[k].getWireIndex())));
            }
        }
    }

    private static void populatePackages(StringEnumerator allStrings, Device device, DeviceResources.Device.Builder devBuilder) {
        Set<String> packages = device.getPackages();
        List<String> packagesList = new ArrayList<String>();
        packagesList.addAll(packages);
        packagesList.sort(new EnumerateCellBelMapping.StringCompare());
        StructList.Builder<DeviceResources.Device.Package.Builder> packagesObj = devBuilder.initPackages(packages.size());

        for (int i = 0; i < packages.size(); ++i) {
            Package pack = device.getPackage(packagesList.get(i));
            DeviceResources.Device.Package.Builder packageBuilder = packagesObj.get(i);

            packageBuilder.setName(allStrings.getIndex(pack.getName()));

            LinkedHashMap<String,PackagePin> packagePinMap = pack.getPackagePinMap();
            List<String> packagePins = new ArrayList<String>();
            packagePins.addAll(packagePinMap.keySet());
            packagePins.sort(new EnumerateCellBelMapping.StringCompare());

            StructList.Builder<DeviceResources.Device.Package.PackagePin.Builder> packagePinsObj = packageBuilder.initPackagePins(packagePins.size());
            for (int j = 0; j < packagePins.size(); ++j) {
                PackagePin packagePin = packagePinMap.get(packagePins.get(j));
                DeviceResources.Device.Package.PackagePin.Builder packagePinObj = packagePinsObj.get(j);

                packagePinObj.setPackagePin(allStrings.getIndex(packagePin.getName()));
                Site site = packagePin.getSite();
                if (site != null) {
                    packagePinObj.initSite().setSite(allStrings.getIndex(site.getName()));
                } else {
                    packagePinObj.initSite().setNoSite(Void.VOID);
                }

                BEL bel = packagePin.getBEL();
                if (bel != null) {
                    packagePinObj.initBel().setBel(allStrings.getIndex(bel.getName()));
                } else {
                    packagePinObj.initBel().setNoBel(Void.VOID);
                }
            }

            StructList.Builder<DeviceResources.Device.Package.Grade.Builder> grades = packageBuilder.initGrades(pack.getGrades().length);
            for (int j = 0; j < pack.getGrades().length; ++j) {
                Grade grade = pack.getGrades()[j];
                DeviceResources.Device.Package.Grade.Builder gradeObj = grades.get(j);
                gradeObj.setName(allStrings.getIndex(grade.getName()));
                gradeObj.setSpeedGrade(allStrings.getIndex(grade.getSpeedGrade()));
                gradeObj.setTemperatureGrade(allStrings.getIndex(grade.getTemperatureGrade()));
            }
        }
    }
*/
}
