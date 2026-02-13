import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { Expense } from '../../models/expense.model';

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

export function exportExpenseReportToPDF({
  expenseData,
  fromDate,
  toDate,
  totalExpenseAmount,
  avgExpenseValue,
  topCategory,
  businessName,
  minAmount,
  maxAmount,
  transactionCount,
  categoryFilter,
  paymentMode,
  bankAccountName
}: {
  expenseData: Expense[],
  fromDate?: string,
  toDate?: string,
  totalExpenseAmount: number,
  avgExpenseValue: number,
  topCategory: string,
  businessName?: string,
  minAmount?: number,
  maxAmount?: number,
  transactionCount?: number,
  categoryFilter?: string,
  paymentMode?: string,
  bankAccountName?: string
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
  doc.text('Expense Report', pageWidth / 2, y, { align: 'center' });
  y += 7;

  // Generated Date (centered, subtle)
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  doc.setTextColor(90);
  doc.text(`Generated on: ${formatDateInIST(new Date())}`, pageWidth / 2, y, { align: 'center' });
  y += 5;

  // Date Range (centered, subtle)
  doc.setFontSize(10);
  if (fromDate || toDate) {
    doc.text(`Date: ${fromDate || '...'} to ${toDate || '...'}`, pageWidth / 2, y, { align: 'center' });
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
  doc.roundedRect(12, y, pageWidth - 24, 20, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  // Grid layout for summary (2 columns)
  doc.text(`Total Expenses:`, 16, y + 13);
  doc.text(`Rs. ${totalExpenseAmount.toLocaleString()}`, 45, y + 13);
  doc.text(`Transactions:`, 80, y + 13);
  doc.text(`${transactionCount !== undefined ? transactionCount : expenseData.length}`, 110, y + 13);
  doc.text(`Avg. Expense:`, 16, y + 18);
  doc.text(`Rs. ${avgExpenseValue.toLocaleString()}`, 45, y + 18);
  doc.text(`Top Category:`, 80, y + 18);
  doc.text(`${topCategory}`, 110, y + 18);
  y += 25;

  // Improved Filters Section (shaded, two-column layout)
  doc.setFillColor(235, 240, 245);
  doc.roundedRect(12, y, pageWidth - 24, 13, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.text('Filters:', 16, y + 6);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  let filterArr = [];
  if (categoryFilter) filterArr.push(`Category: ${categoryFilter}`);
  if (paymentMode) filterArr.push(`Payment Mode: ${paymentMode}`);
  if (bankAccountName) filterArr.push(`Bank: ${bankAccountName}`);
  if (minAmount !== undefined && minAmount !== null) filterArr.push(`Min: Rs. ${minAmount}`);
  if (maxAmount !== undefined && maxAmount !== null) filterArr.push(`Max: Rs. ${maxAmount}`);
  // Two-column filter display
  let filterY = y + 6;
  let col1X = 35, col2X = 90;
  for (let i = 0; i < filterArr.length; i++) {
    const x = i % 2 === 0 ? col1X : col2X;
    const yPos = filterY + Math.floor(i / 2) * 5;
    doc.text(filterArr[i], x, yPos);
  }
  y += 16;

  // Divider line
  doc.setDrawColor(220);
  doc.setLineWidth(0.2);
  doc.line(12, y, pageWidth - 12, y);
  y += 2;

  // Table Data
  const tableData = expenseData.map((expense) => [
    expense.expenseDate ? formatDateInIST(new Date(expense.expenseDate)) : '',
    expense.description,
    expense.category || 'Uncategorized',
    `Rs. ${Number(expense.amount).toLocaleString()}`,
    expense.paymentMode || '-',
    (expense as { bankDetails?: string }).bankDetails || expense.bankAccountNumber || '-',
    expense.notes || '-'
  ]);

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Description',
      'Category',
      'Amount',
      'Payment Mode',
      'Bank Details',
      'Notes'
    ]],
    body: tableData,
    styles: { fontSize: 8.5, cellPadding: 2, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 9, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
    columnStyles: {
      5: { fontSize: 7.5, cellPadding: 1.5 } // Bank Details column - smaller font
    },
    margin: { left: 10, right: 10 },
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

  let fileDate = '';
  if (fromDate || toDate) {
    fileDate = `${fromDate || 'start'}_to_${toDate || 'end'}`;
  } else {
    const now = new Date();
    fileDate = getTodayInIST();
  }
  doc.save(`expense-report-${fileDate}.pdf`);
}
