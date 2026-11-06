import {Component} from '@angular/core';

// Bootstrap's icon font has glyph bearings and font-dependent hinting.
// The cropped SVG gives the dots an explicit geometric bottom for text-baseline alignment.
@Component({
  selector: 'pony-menu-icon',
  templateUrl: './menu-icon.component.svg',
  styleUrls: ['./menu-icon.component.scss']
})
export class MenuIconComponent {
}
