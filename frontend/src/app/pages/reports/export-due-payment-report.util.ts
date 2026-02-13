import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { CustomerDuePayment } from '../../models/customer-due-payment.model';

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

export function exportDuePaymentReportToPDF({
  duePaymentData,
  fromDate,
  toDate,
  customerName,
  totalDueAmount,
  totalSalesAmount,
  totalAmountReceived,
  totalCustomersWithDue,
  averageDueAmount,
  businessName,
  minAmount,
  maxAmount
}: {
  duePaymentData: CustomerDuePayment[],
  fromDate?: string,
  toDate?: string,
  customerName?: string,
  totalDueAmount: number,
  totalSalesAmount: number,
  totalAmountReceived: number,
  totalCustomersWithDue: number,
  averageDueAmount: number,
  businessName?: string,
  minAmount?: number,
  maxAmount?: number
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

  // Report Title
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(15);
  doc.text('Customer Due Payment Report', pageWidth / 2, y, { align: 'center' });
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

  // Summary Section (shaded, grid/table style)
  doc.setFillColor(245, 250, 255);
  doc.roundedRect(12, y, pageWidth - 24, 20, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  // Grid layout for summary (2 columns)
  const formatCurrency = (value: number) => {
    return 'Rs. ' + value.toLocaleString();
  };
  doc.text(`Total Due Amount:`, 16, y + 13);
  doc.text(`${formatCurrency(totalDueAmount)}`, 60, y + 13);
  doc.text(`Total Sales:`, 110, y + 13);
  doc.text(`${formatCurrency(totalSalesAmount)}`, 145, y + 13);
  doc.text(`Amount Received:`, 16, y + 18);
  doc.text(`${formatCurrency(totalAmountReceived)}`, 60, y + 18);
  doc.text(`Customers with Due:`, 110, y + 18);
  doc.text(`${totalCustomersWithDue}`, 145, y + 18);
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
  // Date filter removed to avoid redundancy (date range is shown above)
  filterArr.push(`Customer: ${customerName || 'All'}`);
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

  // Data Table
  const tableData = duePaymentData.map(row => [
    row.customerName || 'N/A',
    row.customerPhone || '-',
    `Rs. ${Number(row.totalSalesAmount || 0).toLocaleString()}`,
    `Rs. ${Number(row.amountReceived || 0).toLocaleString()}`,
    `Rs. ${Number(row.dueAmount || 0).toLocaleString()}`,
    row.lastTransactionDate ? formatDateInIST(new Date(row.lastTransactionDate)) : '-',
    row.transactionCount || 0
  ]);

  autoTable(doc, {
    startY: y + 2,
    head: [['Customer', 'Phone', 'Total Sales', 'Amount Received', 'Due Amount', 'Last Transaction', 'Count']],
    body: tableData,
    styles: { fontSize: 9, cellPadding: 2.2, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 10, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
    columnStyles: {
      0: { halign: 'left' },
      1: { halign: 'center' },
      2: { halign: 'right' },
      3: { halign: 'right' },
      4: { halign: 'right' },
      5: { halign: 'center' },
      6: { halign: 'center' }
    },
    margin: { left: 10, right: 10 },
    didDrawPage: (data) => {
      // Professional footer: left "Confidential", right page number
      const pageCount = doc.getNumberOfPages();
      const pageSize = doc.internal.pageSize;
      const pageHeight = pageSize.getHeight();

      doc.setFontSize(8);
      doc.setTextColor(150);
      doc.text('Confidential', 14, pageHeight - 8);
      doc.text(`Page ${data.pageNumber} of ${pageCount}`, pageSize.getWidth() - 20, pageHeight - 8, { align: 'right' });
    }
  });

  // Save PDF
  const fileName = `due-payment-report-${getTodayInIST()}.pdf`;
  doc.save(fileName);
}
