import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

export function exportSalesReportToPDF({
  salesData,
  fromDate,
  toDate,
  totalSalesAmount,
  avgSaleValue,
  topCustomer,
  customerName,
  variantName,
  businessName,
  minAmount,
  maxAmount,
  transactionCount
}: {
  salesData: any[],
  fromDate?: string,
  toDate?: string,
  totalSalesAmount: number,
  avgSaleValue: number,
  topCustomer: string,
  customerName?: string,
  variantName?: string,
  businessName?: string,
  minAmount?: number,
  maxAmount?: number,
  transactionCount?: number
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
  doc.text('Sales Report', pageWidth / 2, y, { align: 'center' });
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


  // Improved Summary Section (shaded, grid/table style)
  doc.setFillColor(245, 250, 255);
  doc.roundedRect(12, y, pageWidth - 24, 20, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  // Grid layout for summary (2 columns)
  doc.text(`Total Sales:`, 16, y + 13);
  doc.text(`Rs. ${totalSalesAmount.toLocaleString()}`, 45, y + 13);
  doc.text(`Transactions:`, 80, y + 13);
  doc.text(`${transactionCount !== undefined ? transactionCount : salesData.reduce((sum, sale) => sum + (Array.isArray(sale.saleItems) ? sale.saleItems.length : 0), 0)}`, 110, y + 13);
  doc.text(`Avg. Sale Value:`, 16, y + 18);
  doc.text(`Rs. ${avgSaleValue.toLocaleString()}`, 45, y + 18);
  doc.text(`Top Customer:`, 80, y + 18);
  doc.text(`${topCustomer}`, 110, y + 18);
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
  filterArr.push(`Variant: ${variantName || 'All'}`);
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

  // Table Data
  const tableData = salesData.flatMap(sale =>
    (sale.saleItems || []).map((item: any) => [
      sale.saleDate,
      sale.customerName,
      item.variantName,
      item.qtyIssued,
      `Rs. ${Number(item.basePrice).toLocaleString()}`,
      `Rs. ${Number(item.discount).toLocaleString()}`,
      `Rs. ${Number(item.finalPrice).toLocaleString()}`
    ])
  );

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Customer',
      'Variant',
      'Quantity',
      'Base Price',
      'Discount',
      'Amount'
    ]],
    body: tableData,
    styles: { fontSize: 9, cellPadding: 2.2, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 10, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
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
    fileDate = now.toISOString().slice(0, 10);
  }
  doc.save(`sales-report-${fileDate}.pdf`);
}
