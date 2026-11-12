package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.application.dto.XmiExportModel;

/** Puerto de salida para serializar un diagrama como XMI. */
public interface XmiDiagramRenderer {
    byte[] render(XmiExportModel model);
}
