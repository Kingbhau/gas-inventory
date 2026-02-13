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

export function exportDayBookReportToPDF({
  transactions,
  summary,
  selectedDate,
  businessName
}: {
  transactions: DayBook[],
  summary: DayBookSummary,
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

  // Improved Summary Section (shaded, grid/table style)
  doc.setFillColor(245, 250, 255);
  doc.roundedRect(12, y, pageWidth - 24, 28, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);

  // Grid layout for summary (2 columns, 3 rows)
  doc.text(`Total Filled:`, 16, y + 13);
  doc.text(`${summary?.totalFilledCount || 0}`, 50, y + 13);
  doc.text(`Total Returned:`, 80, y + 13);
  doc.text(`${summary?.totalEmptyCount || 0}`, 110, y + 13);

  doc.text(`Total Amount:`, 16, y + 18);
  doc.text(`Rs. ${(summary?.totalAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, 50, y + 18);
  doc.text(`Amount Received:`, 80, y + 18);
  doc.text(`Rs. ${(summary?.totalAmountReceived || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, 110, y + 18);

  doc.text(`Total Due:`, 16, y + 23);
  doc.text(`Rs. ${(summary?.totalDueAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, 50, y + 23);
  doc.text(`Transactions:`, 80, y + 23);
  doc.text(`${summary?.totalTransactions || 0}`, 110, y + 23);

  y += 33;

  // Divider line
  doc.setDrawColor(220);
  doc.setLineWidth(0.2);
  doc.line(12, y, pageWidth - 12, y);
  y += 2;

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
      0: { halign: 'center' },
      1: { halign: 'left' },
      2: { halign: 'left' },
      3: { halign: 'left' },
      4: { halign: 'left' },
      5: { halign: 'right' },
      6: { halign: 'right' },
      7: { halign: 'right' },
      8: { halign: 'left' }
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
