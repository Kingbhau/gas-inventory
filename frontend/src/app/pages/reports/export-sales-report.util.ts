import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { Sale } from '../../models/sale.model';
import { SaleItem } from '../../models/sale-item.model';

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
  referenceNumber,
  transactionCount
}: {
  salesData: Sale[],
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
  referenceNumber?: string,
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
  if (referenceNumber) filterArr.push(`Reference: ${referenceNumber}`);
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

  // Table Data (match screen order: latest first by date, then ID)
  const flattenedItems = salesData
    .flatMap((sale) => (sale.saleItems || []).map((item: SaleItem) => ({ sale, item })))
    .sort((a, b) => {
      const dateA = new Date(a.sale.saleDate).getTime();
      const dateB = new Date(b.sale.saleDate).getTime();
      if (dateB !== dateA) {
        return dateB - dateA;
      }
      return (b.sale.id || 0) - (a.sale.id || 0);
    });

  const tableData = flattenedItems.map(({ sale, item }) => [
    sale.saleDate,
    sale.referenceNumber || '-',
    sale.customerName,
    item.variantName,
    item.qtyIssued,
    `Rs. ${Number(item.basePrice).toLocaleString()}`,
    `Rs. ${Number(item.discount).toLocaleString()}`,
    `Rs. ${Number(item.finalPrice).toLocaleString()}`,
    sale.paymentMode || 'N/A'
  ]);

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Reference #',
      'Customer',
      'Variant',
      'Quantity',
      'Base Price',
      'Discount',
      'Amount',
      'Payment Mode'
    ]],
    body: tableData,
    styles: { fontSize: 8, cellPadding: 2, valign: 'middle', textColor: [40, 40, 40], lineColor: [220, 220, 220], lineWidth: 0.1 },
    headStyles: { fillColor: [44, 62, 80], textColor: 255, fontStyle: 'bold', fontSize: 9, halign: 'center' },
    alternateRowStyles: { fillColor: [245, 250, 255] },
    columnStyles: {
      0: { halign: 'center', cellWidth: 21 },
      1: { halign: 'center', cellWidth: 33 },
      2: { halign: 'left', cellWidth: 23 },
      3: { halign: 'left', cellWidth: 21 },
      4: { halign: 'center', cellWidth: 15 },
      5: { halign: 'right', cellWidth: 21 },
      6: { halign: 'right', cellWidth: 21 },
      7: { halign: 'right', cellWidth: 23 },
      8: { halign: 'left', cellWidth: 22 }
    },
    margin: { left: 5, right: 5 },
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
  doc.save(`sales-report-${fileDate}.pdf`);
}
