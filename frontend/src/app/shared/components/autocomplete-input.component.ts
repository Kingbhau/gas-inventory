import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-autocomplete-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './autocomplete-input.component.html',
  styleUrls: ['./autocomplete-input.component.css']
})
export class AutocompleteInputComponent<T = any> {
  @Input() placeholder: string = '';
  @Input() label: string = '';
  @Input() required: boolean = false;
  @Input() items: T[] = [];
  @Input() displayKey: keyof T | null = null;
  @Input() selected: T | null = null;

  @Output() selectedChange = new EventEmitter<T | null>();

  searchText: string = '';
  isOpen: boolean = false;

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
  }

  onBlur() {
    setTimeout(() => {
      this.isOpen = false;
    }, 150);
  }

  onSelect(item: T) {
    this.selected = item;
    this.selectedChange.emit(item);
    this.searchText = this.getDisplayValue(item);
    this.isOpen = false;
  }

  getDisplayValue(item: T | null): string {
    if (!item) return '';
    if (this.displayKey) {
      return String((item as any)[this.displayKey] ?? '');
    }
    return String(item);
  }
}

