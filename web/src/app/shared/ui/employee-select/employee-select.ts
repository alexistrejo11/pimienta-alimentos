import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { EmployeeLookupService } from '../../../core/employees/employee-lookup.service';
import { EmployeeService } from '../../../core/employees/employee.service';
import type { EmployeeListItemResponse } from '../../../core/model/employee/employee.dto';

@Component({
  selector: 'app-employee-select',
  imports: [FormsModule],
  templateUrl: './employee-select.html',
})
export class EmployeeSelectComponent implements OnInit {
  private readonly service = inject(EmployeeService);
  private readonly lookup = inject(EmployeeLookupService);

  readonly label = input('Colaborador');
  readonly allowNull = input(true);
  readonly nullLabel = input('Selecciona…');
  readonly disabled = input(false);

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();

  readonly options = signal<EmployeeListItemResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.service.listActive(0, 200).subscribe({
      next: (page) => {
        this.options.set(page.items);
        void this.lookup.ensureLoaded();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onChange(id: number | null): void {
    this.valueChange.emit(id);
  }
}
