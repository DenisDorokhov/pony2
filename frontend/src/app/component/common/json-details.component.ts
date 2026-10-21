import {Component, inject, Input, OnInit} from '@angular/core';
import {NgTemplateOutlet} from '@angular/common';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {formatJsonParts, JsonPart} from '../../utils/json.utils';

@Component({
  imports: [TranslateModule, NgTemplateOutlet],
  selector: 'pony-json-details',
  templateUrl: './json-details.component.html',
  styleUrls: ['./json-details.component.scss']
})
export class JsonDetailsComponent implements OnInit {

  readonly activeModal = inject(NgbActiveModal);

  @Input()
  title!: string;

  @Input()
  value!: string;

  parts: JsonPart[] = [];

  ngOnInit(): void {
    this.parts = formatJsonParts(this.value);
  }
}
