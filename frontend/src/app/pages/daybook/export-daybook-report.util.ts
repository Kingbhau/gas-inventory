import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { DayBook, DayBookSummary } from '../../models/daybook.model';

// Helper function for IST date
function getTodayInIST(): string {
  const now = new Date();
  // Get date components in local time (which is IST for Indian users)
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateInIST(date: Date): string {
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

function formatCurrency(value: number | null | undefined): string {
  const amount = Number(value || 0);
  return `Rs. ${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function exportDayBookReportToPDF({
  transactions,
  summary,
  typeSummaries = [],
  filledVariantSummaries = [],
  emptyVariantSummaries = [],
  selectedDate,
  businessName
}: {
  transactions: DayBook[],
  summary: DayBookSummary,
  typeSummaries?: Array<{
    type: string;
    label: string;
    count: number;
    totalAmount: number;
    amountReceived: number;
    dueAmount: number;
    quantityMoved: number;
  }>,
  filledVariantSummaries?: Array<{ variantName: string; filledCount: number }>,
  emptyVariantSummaries?: Array<{ variantName: string; emptyCount: number }>,
  selectedDate: string,
  businessName?: string
}) {
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  let y = 18;

  // Company Name (bold, top center, larger)
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(17);
  doc.text(businessName || 'GAS AGENCY SYSTEM', pageWidth / 2, y, { align: 'center' });
  y += 9;

  // Report Title (modern, clear)
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(15);
  doc.text('Day Book Report', pageWidth / 2, y, { align: 'center' });
  y += 7;

  // Generated Date (centered, subtle)
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  doc.setTextColor(90);
  doc.text(`Generated on: ${formatDateInIST(new Date())}`, pageWidth / 2, y, { align: 'center' });
  y += 5;

  // Date Range (centered, subtle)
  doc.setFontSize(10);
  if (selectedDate) {
    doc.text(`Date: ${selectedDate}`, pageWidth / 2, y, { align: 'center' });
    y += 5;
  }
  doc.setTextColor(0);

  // Divider line
  doc.setDrawColor(180);
  doc.setLineWidth(0.5);
  doc.line(12, y, pageWidth - 12, y);
  y += 4;

  // Combined Summary (single section, 4-column layout)
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(11);
  doc.text('Summary', 12, y + 4);
  y += 7;

  const marginX = 12;
  const gap = 4;
  const columns = 4;
  const colWidth = (pageWidth - (marginX * 2) - gap * (columns - 1)) / columns;
  const filledText = filledVariantSummaries
    .map(v => `${v.variantName} (${v.filledCount || 0})`)
    .join(' · ');
  const returnedText = emptyVariantSummaries
    .map(v => `${v.variantName} (${v.emptyCount || 0})`)
    .join(' · ');

  const drawSummaryItem = (x: number, yPos: number, label: string, value: string, subText?: string) => {
    const height = subText ? 16 : 10;
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(70);
    doc.text(label, x, yPos + 4);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(30);
    doc.text(value, x, yPos + 9);
    if (subText) {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8.5);
      doc.setTextColor(90);
      doc.text(subText, x, yPos + 14);
    }
    doc.setTextColor(0);
    return height;
  };

  const summaryItems: Array<{ label: string; value: string; subText?: string }> = [
    { label: 'Total Filled', value: String(summary?.totalFilledCount || 0), subText: filledText || undefined },
    { label: 'Total Returned', value: String(summary?.totalEmptyCount || 0), subText: returnedText || undefined },
    { label: 'Total Due', value: formatCurrency(summary?.totalDueAmount) },
    { label: 'Transactions', value: String(summary?.totalTransactions || 0) }
  ];

  typeSummaries.forEach((summaryItem) => {
    const amountValue = (summaryItem.type === 'PAYMENT' || summaryItem.type === 'EMPTY_RETURN')
      ? summaryItem.amountReceived
      : summaryItem.totalAmount;
    const valueText = summaryItem.type === 'WAREHOUSE_TRANSFER'
      ? `${summaryItem.quantityMoved} units`
      : formatCurrency(amountValue);
    summaryItems.push({ label: summaryItem.label, value: valueText });
  });

  for (let i = 0; i < summaryItems.length; i += columns) {
    let rowHeight = 0;
    for (let col = 0; col < columns; col++) {
      const item = summaryItems[i + col];
      if (!item) continue;
      const x = marginX + col * (colWidth + gap);
      rowHeight = Math.max(rowHeight, drawSummaryItem(x, y, item.label, item.value, item.subText));
    }
    y += rowHeight + 4;
  }

  // Divider line
  doc.setDrawColor(220);
  doc.setLineWidth(0.2);
  doc.line(12, y, pageWidth - 12, y);
  y += 2;

  doc.setTextColor(0);

  // Table Data
  const tableData = transactions.map((transaction) => [
    transaction.transactionDate,
    (transaction.transactionType || '').replace(/_/g, ' ') || 'Transaction',
    transaction.partyName || '-',
    transaction.referenceNumber || '-',
    transaction.details || '-',
    `Rs. ${Number(transaction.totalAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    `Rs. ${Number(transaction.amountReceived || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    `Rs. ${Number(transaction.dueAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    transaction.paymentMode || 'N/A'
  ]);

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Type',
      'Party',
      'Reference',
      'Details',
      'Amount',
      'Received',
      'Due',
      'Mode'
    ]],
    body: tableData,
    styles: { fontSize: 7.5, cellPadding: 1.5, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 8, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
    columnStyles: {
      0: { halign: 'center', cellWidth: 18 }, // Date
      1: { halign: 'left', cellWidth: 16 },   // Type
      2: { halign: 'left', cellWidth: 16 },   // Party
      3: { halign: 'left', cellWidth: 31 },   // Reference
      4: { halign: 'left', cellWidth: 40 },   // Details
      5: { halign: 'right', cellWidth: 23 },  // Amount
      6: { halign: 'right', cellWidth: 23 },  // Received
      7: { halign: 'right', cellWidth: 23 },  // Due
      8: { halign: 'left', cellWidth: 12 }    // Mode
    },
    margin: { left: 5, right: 5, top: 10 },
    didDrawPage: (data) => {
      // Professional footer: left "Confidential", right page number
      const pageCount = doc.getNumberOfPages();
      const pageNumber = doc.getCurrentPageInfo().pageNumber;
      doc.setFontSize(8);
      doc.setTextColor(120);
      doc.text('Confidential', 12, pageHeight - 8);
      doc.setTextColor(150);
      doc.text(`Page ${pageNumber} of ${pageCount}`, pageWidth - 12, pageHeight - 8, { align: 'right' });
    }
  });

  const fileDate = selectedDate || getTodayInIST();
  doc.save(`daybook-report-${fileDate}.pdf`);
}
