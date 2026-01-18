import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

export function exportCustomerLedgerToPDF({
  customerName,
  customerPhone,
  ledgerData,
  totalDebit,
  totalCredit,
  balanceDue,
  businessName,
  fromDate,
  toDate
}: {
  customerName: string,
  customerPhone?: string,
  ledgerData: any[],
  totalDebit: number,
  totalCredit: number,
  balanceDue: number,
  businessName?: string,
  fromDate?: string,
  toDate?: string
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
  doc.text('Customer Ledger', pageWidth / 2, y, { align: 'center' });
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
    doc.text(`Period: ${fromDate || '...'} to ${toDate || '...'}`, pageWidth / 2, y, { align: 'center' });
    y += 5;
  }
  doc.setTextColor(0);

  // Divider line
  doc.setDrawColor(180);
  doc.setLineWidth(0.5);
  doc.line(12, y, pageWidth - 12, y);
  y += 4;

  // Customer Information Section (shaded)
  doc.setFillColor(245, 250, 255);
  doc.roundedRect(12, y, pageWidth - 24, 16, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Customer Information', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  doc.text(`Name:`, 16, y + 13);
  doc.text(`${customerName}`, 45, y + 13);
  if (customerPhone) {
    doc.text(`Phone:`, 100, y + 13);
    doc.text(`${customerPhone}`, 130, y + 13);
  }
  y += 21;

  // Summary Section (shaded)
  doc.setFillColor(235, 240, 245);
  doc.roundedRect(12, y, pageWidth - 24, 19, 2, 2, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10.5);
  doc.text('Account Summary', 16, y + 7);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9.5);
  
  // Summary in 2 columns
  doc.text(`Total Sales:`, 16, y + 13);
  doc.text(`Rs. ${totalDebit.toLocaleString()}`, 50, y + 13);
  doc.text(`Total Payments:`, 105, y + 13);
  doc.text(`Rs. ${totalCredit.toLocaleString()}`, 140, y + 13);
  
  doc.text(`Balance Due:`, 16, y + 18);
  doc.setFont('helvetica', 'bold');
  const textColor = balanceDue > 0 ? [220, 38, 38] : [34, 197, 94]; // Red if due, green if credit
  doc.setTextColor(textColor[0], textColor[1], textColor[2]);
  doc.text(`Rs. ${balanceDue.toLocaleString()}`, 50, y + 18);
  doc.setTextColor(0);
  doc.setFont('helvetica', 'normal');
  
  y += 24;

  // Divider line
  doc.setDrawColor(220);
  doc.setLineWidth(0.2);
  doc.line(12, y, pageWidth - 12, y);
  y += 2;

  // Ledger Table
  const tableData = ledgerData.map((entry: any, index: number) => {
    // Calculate running balance (cumulative)
    let runningBalance = 0;
    for (let i = 0; i <= index; i++) {
      if (ledgerData[i].refType === 'PAYMENT' || ledgerData[i].refType === 'CREDIT') {
        runningBalance -= ledgerData[i].amountReceived || 0;
      } else {
        runningBalance += ledgerData[i].totalAmount || 0;
      }
    }

    // Variant/Mode column
    let variantMode = '';
    if (entry.variantName) {
      variantMode = entry.variantName;
    } else if (entry.paymentMode) {
      variantMode = `By ${entry.paymentMode}`;
    } else {
      variantMode = 'PAYMENT';
    }

    return [
      new Date(entry.transactionDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }),
      entry.transactionReference || '-',
      variantMode,
      entry.refType || 'N/A',
      entry.filledOut || 0,
      entry.emptyIn || 0,
      entry.balance || 0,
      `Rs ${Number(entry.totalAmount || 0).toLocaleString()}`,
      `Rs ${Number(entry.amountReceived || 0).toLocaleString()}`,
      `Rs ${Number(runningBalance).toLocaleString()}`
    ];
  });

  autoTable(doc, {
    startY: y + 2,
    head: [[
      'Date',
      'Reference #',
      'Variant / Mode',
      'Type',
      'Filled Out',
      'Empty In',
      'Balance',
      'Total Amount (Rs)',
      'Amount Received (Rs)',
      'Running Balance (Rs)'
    ]],
    body: tableData,
    styles: {
      fontSize: 8,
      cellPadding: 2,
      valign: 'middle',
      textColor: [40, 40, 40],
      lineColor: [220, 220, 220],
      lineWidth: 0.1
    },
    headStyles: {
      fillColor: [44, 62, 80],
      textColor: 255,
      fontStyle: 'bold',
      fontSize: 9,
      halign: 'center'
    },
    alternateRowStyles: {
      fillColor: [245, 250, 255]
    },
    columnStyles: {
      0: { halign: 'center', cellWidth: 18 },
      1: { halign: 'center', cellWidth: 32 },
      2: { halign: 'left', cellWidth: 20 },
      3: { halign: 'center', cellWidth: 30 },
      4: { halign: 'center', cellWidth: 13 },
      5: { halign: 'center', cellWidth: 13 },
      6: { halign: 'center', cellWidth: 14 },
      7: { halign: 'right', cellWidth: 20 },
      8: { halign: 'right', cellWidth: 20 },
      9: { halign: 'right', cellWidth: 20 }
    },
    margin: { left: 5, right: 5, top: 5, bottom: 5 },
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

  // Generate filename with date range
  let fileDate = '';
  if (fromDate || toDate) {
    fileDate = `${fromDate || 'start'}_to_${toDate || 'end'}`;
  } else {
    const now = new Date();
    fileDate = now.toISOString().slice(0, 10);
  }
  
  const sanitizedName = customerName.replace(/[^a-z0-9]/gi, '_').toLowerCase();
  doc.save(`ledger-${sanitizedName}-${fileDate}.pdf`);
}
