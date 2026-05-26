package com.pyrosense.reporting.application.port.out;

import com.pyrosense.reporting.domain.model.ReportViewModel;

public interface PdfRendererPort {

    byte[] render(ReportViewModel viewModel);
}
