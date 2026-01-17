import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

export function exportBankTransactionsReportToPDF({
  transactions,
  fromDate,
  toDate,
  bankAccountName,
  transactionType,
  totalDeposits,
  totalWithdrawals,
  netChange,
  currentBalance,
  transactionCount,
  businessName
}: {
  transactions: any[],
  fromDate?: string,
  toDate?: string,
  bankAccountName?: string,
  transactionType?: string,
  totalDeposits: number,
  totalWithdrawals: number,
  netChange: number,
  currentBalance: number,
  transactionCount: number,
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
  doc.text('Bank Transactions Report', pageWidth / 2, y, { align: 'center' });
  y += 7;

  // Generated Date (centered, subtle)
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  doc.setTextColor(90);
  doc.text(`Generated on: ${new Date().toLocaleDateString()}`, pageWidth / 2, y, { align: 'center' });
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
  doc.roundedRect(12, y, pageWidth - 24, 25, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  
  // Grid layout for summary (2 columns)
  doc.text(`Total Deposits:`, 16, y + 13);
  doc.text(`Rs. ${totalDeposits.toLocaleString()}`, 45, y + 13);
  doc.text(`Current Balance:`, 80, y + 13);
  doc.text(`Rs. ${currentBalance.toLocaleString()}`, 110, y + 13);
  
  doc.text(`Total Withdrawals:`, 16, y + 18);
  doc.text(`Rs. ${totalWithdrawals.toLocaleString()}`, 45, y + 18);
  doc.text(`Transactions:`, 80, y + 18);
  doc.text(`${transactionCount}`, 110, y + 18);
  
  doc.text(`Net Change:`, 16, y + 23);
  doc.text(`Rs. ${netChange.toLocaleString()}`, 45, y + 23);
  
  y += 30;

  // Filters Section (shaded, two-column layout)
  doc.setFillColor(235, 240, 245);
  doc.roundedRect(12, y, pageWidth - 24, 13, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.text('Filters:', 16, y + 6);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  
  let filterArr = [];
  filterArr.push(`Bank: ${bankAccountName || 'All'}`);
  filterArr.push(`Type: ${transactionType || 'All'}`);
  
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
  const tableData = transactions.map(row => [
    row.transactionDate ? new Date(row.transactionDate).toLocaleDateString('en-IN') : '-',
    row.bankAccountName?.split(' - ')[0] || row.bankAccountName || '-',
    row.transactionType || '-',
    `Rs. ${Number(row.amount).toLocaleString()}`,
    `Rs. ${Number(row.balanceAfter).toLocaleString()}`,
    row.referenceNumber || '-'
  ]);

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Bank Account',
      'Type',
      'Amount',
      'Balance',
      'Reference'
    ]],
    body: tableData,
    styles: { fontSize: 9, cellPadding: 2.2, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 10, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
    margin: { left: 10, right: 10 },
    columnStyles: {
      0: { halign: 'center', cellWidth: 25 },
      1: { halign: 'left', cellWidth: 30 },
      2: { halign: 'center', cellWidth: 22 },
      3: { halign: 'right', cellWidth: 28 },
      4: { halign: 'right', cellWidth: 28 },
      5: { halign: 'center', cellWidth: 27 }
    },
    didDrawPage: (data: any) => {
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

  // Generate filename
  const timestamp = new Date().toLocaleDateString('en-IN').replace(/\//g, '-');
  const filename = `Bank_Transactions_${timestamp}.pdf`;

  // Save the PDF
  doc.save(filename);
}
