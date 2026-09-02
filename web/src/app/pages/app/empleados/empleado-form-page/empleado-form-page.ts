import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeService } from '../../../../core/employees/employee.service';
import { fieldMessage, parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ContractType, EmployeeOnboardingPhase, WorkShift } from '../../../../core/model/employee/employee.enums';
import type {
  EmployeeResponse,
  RegisterEmployeeRequest,
  UpdateEmployeeRequest,
} from '../../../../core/model/employee/employee.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

const CONTRACT_TYPES: ContractType[] = [
  'INDEFINITE',
  'FIXED_TERM',
  'PROJECT_BASED',
  'TEMPORARY',
  'FREELANCE',
];

const WORK_SHIFTS: WorkShift[] = ['MORNING', 'AFTERNOON', 'NIGHT', 'MIXED', 'REMOTE'];

const ONBOARDING_PHASES: EmployeeOnboardingPhase[] = ['DRAFT', 'PENDING_CONTRACT'];

@Component({
  selector: 'app-empleado-form-page',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './empleado-form-page.html',
})
export class EmpleadoFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EmployeeService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly employeeId = signal<number | null>(null);
  readonly photoFile = signal<File | null>(null);

  readonly contractTypes = CONTRACT_TYPES;
  readonly workShifts = WORK_SHIFTS;
  readonly onboardingPhases = ONBOARDING_PHASES;

  readonly contractTypeLabel: Record<ContractType, string> = {
    INDEFINITE: 'Indefinido',
    FIXED_TERM: 'Plazo fijo',
    PROJECT_BASED: 'Por proyecto',
    TEMPORARY: 'Temporal',
    FREELANCE: 'Freelance',
  };

  readonly workShiftLabel: Record<WorkShift, string> = {
    MORNING: 'Matutino',
    AFTERNOON: 'Vespertino',
    NIGHT: 'Nocturno',
    MIXED: 'Mixto',
    REMOTE: 'Remoto',
  };

  readonly onboardingPhaseLabel: Record<EmployeeOnboardingPhase, string> = {
    DRAFT: 'Borrador',
    PENDING_CONTRACT: 'Pendiente de contrato',
  };

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required]],
    address: ['', [Validators.required]],
    curp: ['', [Validators.required, Validators.minLength(18), Validators.maxLength(18)]],
    rfc: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(13)]],
    nss: ['', [Validators.required]],
    clabe: ['', [Validators.required, Validators.minLength(18), Validators.maxLength(18)]],
    employeeNumber: ['', [Validators.required]],
    position: ['', [Validators.required]],
    department: ['', [Validators.required]],
    contractType: this.fb.nonNullable.control<ContractType>('INDEFINITE', [Validators.required]),
    workShift: this.fb.nonNullable.control<WorkShift>('MORNING', [Validators.required]),
    salaryPerWeek: ['', [Validators.required, Validators.min(0)]],
    birthDate: ['', [Validators.required]],
    onboardingPhase: this.fb.nonNullable.control<EmployeeOnboardingPhase>('DRAFT', [Validators.required]),
    bonuses: ['0', [Validators.required, Validators.min(0)]],
    foodVouchers: ['0', [Validators.required, Validators.min(0)]],
    integrationFactor: ['1', [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    const isEditRoute = this.route.snapshot.url.some((u) => u.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;
    if (idRaw) {
      const id = Number(idRaw);
      if (!Number.isFinite(id) || id <= 0) {
        void this.router.navigateByUrl('/app/empleados');
        return;
      }
      this.employeeId.set(id);
      this.clearCreateOnlyValidators();
      this.loadEmployee(id);
    }
  }

  get isEdit(): boolean {
    return this.employeeId() != null;
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.photoFile.set(input.files?.[0] ?? null);
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.employeeId();
    this.loading.set(true);
    const photo = this.photoFile() ?? undefined;

    if (id == null) {
      this.service
        .register(this.buildRegisterBody(), photo)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (created) => void this.router.navigateByUrl(`/app/empleados/${created.id}`),
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    } else {
      this.service
        .update(id, this.buildUpdateBody(), photo)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: () => void this.router.navigateByUrl(`/app/empleados/${id}`),
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    }
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }

  private clearCreateOnlyValidators(): void {
    this.form.controls.employeeNumber.clearValidators();
    this.form.controls.birthDate.clearValidators();
    this.form.controls.onboardingPhase.clearValidators();
    this.form.controls.employeeNumber.updateValueAndValidity();
    this.form.controls.birthDate.updateValueAndValidity();
    this.form.controls.onboardingPhase.updateValueAndValidity();
  }

  private loadEmployee(id: number): void {
    this.loadingExisting.set(true);
    this.apiError.set(null);
    this.service.getById(id).subscribe({
      next: (e: EmployeeResponse) => {
        this.patchFromEmployee(e);
        this.loadingExisting.set(false);
      },
      error: (err: unknown) => {
        this.apiError.set(parseApiError(err));
        this.loadingExisting.set(false);
      },
    });
  }

  private patchFromEmployee(e: EmployeeResponse): void {
    this.form.patchValue({
      firstName: e.firstName,
      lastName: e.lastName,
      email: e.email,
      phone: e.phone,
      address: e.address,
      curp: e.curp,
      rfc: e.rfc,
      nss: e.nss,
      clabe: e.clabe,
      position: e.position,
      department: e.department,
      contractType: e.contractType,
      workShift: e.workShift,
      salaryPerWeek: String(e.salaryPerWeek),
      bonuses: String(e.bonuses),
      foodVouchers: String(e.foodVouchers),
      integrationFactor: String(e.integrationFactor),
    });
  }

  private buildRegisterBody(): RegisterEmployeeRequest {
    const v = this.form.getRawValue();
    return {
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      email: v.email.trim(),
      phone: v.phone.trim(),
      address: v.address.trim(),
      curp: v.curp.trim(),
      rfc: v.rfc.trim(),
      nss: v.nss.trim(),
      clabe: v.clabe.trim(),
      employeeNumber: v.employeeNumber.trim(),
      position: v.position.trim(),
      department: v.department.trim(),
      contractType: v.contractType,
      workShift: v.workShift,
      salaryPerWeek: Number(v.salaryPerWeek),
      birthDate: v.birthDate,
      onboardingPhase: v.onboardingPhase,
    };
  }

  private buildUpdateBody(): UpdateEmployeeRequest {
    const v = this.form.getRawValue();
    return {
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      email: v.email.trim(),
      phone: v.phone.trim(),
      address: v.address.trim(),
      curp: v.curp.trim(),
      rfc: v.rfc.trim(),
      nss: v.nss.trim(),
      clabe: v.clabe.trim(),
      position: v.position.trim(),
      department: v.department.trim(),
      contractType: v.contractType,
      workShift: v.workShift,
      salaryPerWeek: Number(v.salaryPerWeek),
      bonuses: Number(v.bonuses),
      foodVouchers: Number(v.foodVouchers),
      integrationFactor: Number(v.integrationFactor),
    };
  }
}
