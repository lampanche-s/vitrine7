import type {
  Workbook,
  Worksheet,
} from "exceljs";

export type ReportColumn = {
  key: string;
  header: string;
  width?: number;
  type?: "text" | "number" | "currency" | "date";
};

export type ReportRow = Record<
  string,
  string | number | Date | null | undefined
>;

export type ReportMetric = {
  label: string;
  value: string;
};

export type ReportTable = {
  sheetName: string;
  title: string;
  columns: ReportColumn[];
  rows: ReportRow[];
};

export type ReportWorkbookInput = {
  fileName: string;
  title: string;
  subtitle: string;
  generatedAt: Date;
  metrics: ReportMetric[];
  tables: ReportTable[];
};

function sanitizeSheetName(name: string) {
  return name
    .replace(/[\\/*?:[\]]/g, "")
    .slice(0, 31);
}

function formatFileDate(date: Date) {
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");

  return `${year}-${month}-${day}-${hour}${minute}`;
}

function applyBaseSheetStyle(sheet: Worksheet) {
  sheet.properties.defaultRowHeight = 22;
  sheet.views = [
    {
      state: "frozen",
      ySplit: 1,
    },
  ];
}

function styleTitleRow(sheet: Worksheet, rowNumber: number) {
  const row = sheet.getRow(rowNumber);

  row.height = 28;

  row.eachCell((cell) => {
    cell.font = {
      bold: true,
      color: { argb: "FFFFFFFF" },
      size: 14,
    };

    cell.fill = {
      type: "pattern",
      pattern: "solid",
      fgColor: { argb: "FF111111" },
    };

    cell.alignment = {
      vertical: "middle",
    };
  });
}

function styleHeaderRow(sheet: Worksheet, rowNumber: number) {
  const row = sheet.getRow(rowNumber);

  row.height = 24;

  row.eachCell((cell) => {
    cell.font = {
      bold: true,
      color: { argb: "FFFFFFFF" },
      size: 11,
    };

    cell.fill = {
      type: "pattern",
      pattern: "solid",
      fgColor: { argb: "FFB98A2E" },
    };

    cell.border = {
      top: { style: "thin", color: { argb: "FFE5E7EB" } },
      bottom: { style: "thin", color: { argb: "FFE5E7EB" } },
    };

    cell.alignment = {
      vertical: "middle",
    };
  });
}

function styleDataRows(sheet: Worksheet, startRow: number, endRow: number) {
  for (let rowNumber = startRow; rowNumber <= endRow; rowNumber += 1) {
    const row = sheet.getRow(rowNumber);

    row.eachCell((cell) => {
      cell.font = {
        color: { argb: "FF111111" },
        size: 10,
      };

      cell.border = {
        bottom: { style: "thin", color: { argb: "FFE5E7EB" } },
      };

      cell.alignment = {
        vertical: "middle",
      };

      if (rowNumber % 2 === 0) {
        cell.fill = {
          type: "pattern",
          pattern: "solid",
          fgColor: { argb: "FFF8F8F8" },
        };
      }
    });
  }
}

function addSummarySheet(
  workbook: Workbook,
  input: ReportWorkbookInput
) {
  const sheet = workbook.addWorksheet("Resumo");

  applyBaseSheetStyle(sheet);

  sheet.columns = [
    { width: 28 },
    { width: 22 },
    { width: 28 },
    { width: 22 },
  ];

  sheet.mergeCells("A1:D1");
  sheet.getCell("A1").value = input.title;
  styleTitleRow(sheet, 1);

  sheet.mergeCells("A2:D2");
  sheet.getCell("A2").value = input.subtitle;
  sheet.getCell("A2").font = {
    color: { argb: "FF6B7280" },
    size: 10,
  };

  sheet.getCell("A4").value = "Gerado em";
  sheet.getCell("B4").value = input.generatedAt.toLocaleString("pt-BR");

  sheet.getRow(6).values = [
    "Indicador",
    "Valor",
    "Indicador",
    "Valor",
  ];

  styleHeaderRow(sheet, 6);

  let rowNumber = 7;

  for (let index = 0; index < input.metrics.length; index += 2) {
    const left = input.metrics[index];
    const right = input.metrics[index + 1];

    sheet.getRow(rowNumber).values = [
      left?.label ?? "",
      left?.value ?? "",
      right?.label ?? "",
      right?.value ?? "",
    ];

    rowNumber += 1;
  }

  styleDataRows(sheet, 7, rowNumber - 1);

  sheet.getCell(`A${rowNumber + 2}`).value = "Abas do arquivo";
  sheet.getCell(`A${rowNumber + 2}`).font = {
    bold: true,
    color: { argb: "FF111111" },
  };

  input.tables.forEach((table, index) => {
    sheet.getCell(`A${rowNumber + 3 + index}`).value =
      table.sheetName;

    sheet.getCell(`B${rowNumber + 3 + index}`).value =
      `${table.rows.length} registro(s)`;
  });
}

function addTableSheet(
  workbook: Workbook,
  table: ReportTable
) {
  const sheet = workbook.addWorksheet(
    sanitizeSheetName(table.sheetName)
  );

  applyBaseSheetStyle(sheet);

  sheet.columns = table.columns.map((column) => ({
    key: column.key,
    header: column.header,
    width: column.width ?? 20,
  }));

  sheet.insertRow(1, [table.title]);
  sheet.mergeCells(1, 1, 1, table.columns.length);
  styleTitleRow(sheet, 1);

  sheet.insertRow(2, table.columns.map((column) => column.header));
  styleHeaderRow(sheet, 2);

  table.rows.forEach((row) => {
    const values = table.columns.map((column) => row[column.key] ?? "");
    sheet.addRow(values);
  });

  const firstDataRow = 3;
  const lastDataRow = Math.max(3, sheet.rowCount);

  styleDataRows(sheet, firstDataRow, lastDataRow);

  sheet.autoFilter = {
    from: {
      row: 2,
      column: 1,
    },
    to: {
      row: 2,
      column: table.columns.length,
    },
  };

  table.columns.forEach((column, columnIndex) => {
    const excelColumn = sheet.getColumn(columnIndex + 1);

    if (column.type === "currency") {
      excelColumn.numFmt = '"R$" #,##0.00';
    }

    if (column.type === "date") {
      excelColumn.numFmt = "dd/mm/yyyy hh:mm";
    }
  });
}

export async function exportReportWorkbook(input: ReportWorkbookInput) {
  const ExcelJS = await import("exceljs");

  const workbook = new ExcelJS.Workbook();

  workbook.creator = "Vitrine 7";
  workbook.created = input.generatedAt;
  workbook.modified = input.generatedAt;

  addSummarySheet(workbook, input);

  input.tables.forEach((table) => {
    addTableSheet(workbook, table);
  });

  const buffer = await workbook.xlsx.writeBuffer();

  const blob = new Blob([buffer as BlobPart], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `${input.fileName}-${formatFileDate(input.generatedAt)}.xlsx`;
  link.click();

  URL.revokeObjectURL(url);
}
