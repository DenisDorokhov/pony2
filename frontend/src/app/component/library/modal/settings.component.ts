import {Component, OnInit} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {ConfigService} from "../../../service/config.service";
import {LoadingState} from "../../../domain/common.model";
import {ConfigDto} from "../../../domain/config.dto";
import {FormArray, FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {ErrorDto} from "../../../domain/common.dto";
import {ErrorComponent} from "../../common/error.component";
import {AutoFocusDirective} from "../../common/auto-focus.directive";
import {ErrorContainerComponent} from "../../common/error-container.component";
import {DatePipe, NgForOf} from "@angular/common";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";

@Component({
  standalone: true,
  imports: [TranslateModule, ErrorComponent, ReactiveFormsModule, AutoFocusDirective, ErrorContainerComponent, NgForOf, ErrorIndicatorComponent, LoadingIndicatorComponent, DatePipe],
  selector: 'pony-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit{

  LoadingState = LoadingState;

  config: ConfigDto | undefined;

  form: FormGroup;
  formLibraryFolders: FormArray;
  error: ErrorDto | undefined;

  loadingState = LoadingState.LOADING;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly configService: ConfigService,
    private readonly formBuilder: FormBuilder,
  ) {
    this.formLibraryFolders = formBuilder.array([
      formBuilder.group({path: ''})
    ]);
    this.form = formBuilder.group({
      libraryFolders: this.formLibraryFolders,
    });
  }

  ngOnInit(): void {
    this.configService.getConfig().subscribe({
      next: config => {
        this.config = config;
        this.formLibraryFolders.clear();
        this.config.libraryFolders.forEach(next =>
          this.formLibraryFolders.push(this.formBuilder.group({path: next.path})));
        this.loadingState = LoadingState.LOADED;
      },
      error: () => {
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  addLibraryFolder() {
    this.formLibraryFolders.push(this.formBuilder.group({path: ''}));
  }

  removeLibraryFolder(i: number) {
    this.formLibraryFolders.removeAt(i);
  }

  save() {
    const configToSave = <ConfigDto>this.form.value;
    this.loadingState = LoadingState.LOADING;
    this.configService.saveConfig(configToSave).subscribe({
      next: config => {
        this.loadingState = LoadingState.LOADED;
        this.activeModal.close(config);
      },
      error: error => {
        this.error = error;
        this.loadingState = this.error?.code === ErrorDto.Code.VALIDATION ? LoadingState.LOADED : LoadingState.ERROR;
      }
    })
  }
}
