import {DestroyRef, Directive, ElementRef, inject, NgZone} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {fromEvent, merge} from 'rxjs';

@Directive({
  selector: 'button[ponySelectableButton]'
})
export class SelectableButtonDirective {

  private readonly element = inject<ElementRef<HTMLButtonElement>>(ElementRef).nativeElement;
  private selectionGesture = false;

  constructor() {
    const destroyRef = inject(DestroyRef);
    const ngZone = inject(NgZone);

    this.setTextSelection(false);
    ngZone.runOutsideAngular(() => {
      fromEvent<MouseEvent>(this.element, 'mousedown', {capture: true})
        .pipe(takeUntilDestroyed(destroyRef))
        .subscribe(event => this.onMouseDown(event));

      merge(
        fromEvent<MouseEvent>(this.element, 'click', {capture: true}),
        fromEvent<MouseEvent>(this.element, 'dblclick', {capture: true}),
        fromEvent<DragEvent>(this.element, 'dragstart', {capture: true}),
      ).pipe(takeUntilDestroyed(destroyRef))
        .subscribe(event => this.onSelectionAction(event));
    });
  }

  private setTextSelection(enabled: boolean) {
    const value = enabled ? 'text' : 'none';
    this.element.style.setProperty('user-select', value);
    this.element.style.setProperty('-webkit-user-select', value);
  }

  private onMouseDown(event: MouseEvent) {
    if (event.button !== 0) {
      return;
    }
    this.selectionGesture = event.altKey;
    this.setTextSelection(this.selectionGesture);
    if (this.selectionGesture) {
      event.stopPropagation();
    }
  }

  private onSelectionAction(event: MouseEvent) {
    // Keyboard and programmatic clicks have no mouse click count.
    const selectionAction = this.selectionGesture && (event.detail > 0 || event.type === 'dragstart');
    if (event.altKey || selectionAction) {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  }
}
