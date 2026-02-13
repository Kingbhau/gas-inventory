import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoadingState } from '../models/loading-state.model';

export { LoadingState };

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingSubject = new BehaviorSubject<LoadingState>({ isLoading: false });
  public loading$: Observable<LoadingState> = this.loadingSubject.asObservable();

  private requestCount = 0;

  show(message: string = 'Loading...') {
    this.requestCount++;
    this.loadingSubject.next({ isLoading: true, message });
  }

  hide() {
    this.requestCount = Math.max(0, this.requestCount - 1);
    if (this.requestCount === 0) {
      this.loadingSubject.next({ isLoading: false });
    }
  }

  reset() {
    this.requestCount = 0;
    this.loadingSubject.next({ isLoading: false });
  }
}
