import { Component, input, output } from '@angular/core';

import type { ChecklistItemResponse } from '../../../../../core/model/task/task.dto';

/**
 * Sección de checklist de solo lectura.
 * Muestra cada ítem con su estado (completado o pendiente).
 */
@Component({
  selector: 'app-checklist-section',
  
  templateUrl: './checklist-section.html',
})
export class ChecklistSectionComponent {
  readonly items = input.required<ChecklistItemResponse[]>();
  readonly readonly = input(true);
  readonly toggleItem = output<number>();

  onToggle(displayOrder: number): void {
    if (this.readonly()) return;
    this.toggleItem.emit(displayOrder);
  }
}
