import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

export function exportSupplierTransactionsToPDF({
  transactions,
  fromDate,
  toDate,
  businessName,
  supplierName,
  variantName
}: {
  transactions: any[],
  fromDate?: string,
  toDate?: string,
  businessName?: string,
  supplierName?: string,
  variantName?: string
}) {
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  let y = 18;

  // Company Name
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(17);
  doc.text(businessName || 'GAS AGENCY SYSTEM', pageWidth / 2, y, { align: 'center' });
  y += 9;

  // Report Title
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(15);
  doc.text('Supplier Transactions Report', pageWidth / 2, y, { align: 'center' });
  y += 7;

  // Generated Date
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  doc.setTextColor(90);
  doc.text(`Generated on: ${new Date().toLocaleDateString()}`, pageWidth / 2, y, { align: 'center' });
  y += 5;

  // Date Range
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

  // Summary Section
  doc.setFillColor(245, 250, 255);
  doc.roundedRect(12, y, pageWidth - 24, 14, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  doc.text(`Total Transactions: ${transactions.length}`, 45, y + 7);
  const totalAmount = transactions.reduce((sum, t) => sum + (Number(t.amount) || 0), 0);
  doc.text(`Total Amount: Rs. ${totalAmount.toLocaleString()}`, 110, y + 7);
  y += 16;

  // Filters Section (shaded, two-column layout)
  doc.setFillColor(235, 240, 245);
  doc.roundedRect(12, y, pageWidth - 24, 13, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.text('Filters:', 16, y + 6);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(8.5);
  let filterArr = [];
  filterArr.push(`Supplier: ${supplierName || 'All'}`);
  filterArr.push(`Variant: ${variantName || 'All'}`);
  if (fromDate || toDate) filterArr.push(`Date: ${fromDate || '...'} to ${toDate || '...'}`);
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
  const tableData = transactions.map(t => [
    t.transactionDate,
    t.warehouseName,
    t.supplierName,
    t.variantName,
    t.filledReceived,
    t.emptySent,
    `Rs. ${Number(t.amount).toLocaleString()}`
  ]);

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Warehouse',
      'Supplier',
      'Variant',
      'Filled Received',
      'Empty Sent',
      'Amount'
    ]],
    body: tableData,
    styles: { fontSize: 9, cellPadding: 2.2, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 10, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
    margin: { left: 10, right: 10 },
    didDrawPage: (data) => {
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
    fileDate = now.toISOString().slice(0, 10);
  }
  doc.save(`supplier-transactions-report-${fileDate}.pdf`);
}
