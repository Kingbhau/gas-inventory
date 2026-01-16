import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy, ChangeDetectorRef, OnChanges, SimpleChanges, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-autocomplete-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './autocomplete-input.component.html',
  styleUrls: ['./autocomplete-input.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AutocompleteInputComponent<T = any> implements OnChanges {
  @Input() placeholder: string = '';
  @Input() label: string = '';
  @Input() required: boolean = false;
  @Input() items: T[] = [];
  @Input() displayKey: keyof T | null = null;
  @Input() selected: T | null = null;

  @Output() selectedChange = new EventEmitter<T | null>();
  @ViewChild('searchInput') searchInput!: ElementRef;

  searchText: string = '';
  isOpen: boolean = false;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['items'] && !changes['items'].firstChange) {
      this.cdr.markForCheck();
    }
  }

  get filteredItems(): T[] {
    const text = (this.searchText || '').toLowerCase();
    if (!text) {
      return this.items || [];
    }
    return (this.items || []).filter((item: any) => {
      const val = this.displayKey ? item[this.displayKey] : item;
      return String(val || '').toLowerCase().includes(text);
    });
  }

  onFocus() {
    this.isOpen = true;
    this.cdr.markForCheck();
  }

  onBlur() {
    setTimeout(() => {
      this.isOpen = false;
      this.cdr.markForCheck();
    }, 150);
  }

  onInputChange() {
    // If input is cleared, emit null to clear the form value
    if (!this.searchText || this.searchText.trim() === '') {
      this.selected = null;
      this.selectedChange.emit(null);
      this.cdr.markForCheck();
    }
  }

  onSelect(item: T) {
    this.selected = item;
    this.selectedChange.emit(item);
    this.searchText = this.getDisplayValue(item);
    this.isOpen = false;
    this.cdr.markForCheck();
  }

  getDisplayValue(item: T | null): string {
    if (!item) return '';
    if (this.displayKey) {
      return String((item as any)[this.displayKey] ?? '');
    }
    return String(item);
  }

  resetInput(): void {
    this.searchText = '';
    this.selected = null;
    this.isOpen = false;
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
    this.cdr.markForCheck();
  }
}

