import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';
import {AuthenticationService} from '../core/user/authentication.service';

@Component({
  selector: 'pony-library',
  templateUrl: './library.component.html',
  styleUrls: ['./library.component.scss']
})
export class LibraryComponent implements OnInit, OnDestroy {

  private loggedOutSubscription: Subscription;

  constructor(private authenticationService: AuthenticationService, private router: Router) {
  }

  ngOnInit(): void {
    this.loggedOutSubscription = this.authenticationService.loggedOut.subscribe(
      user => {
        console.log(`User ${user.email} has been logged out.`);
        this.router.navigate(['/login'], {replaceUrl: true});
      }
    );
  }

  ngOnDestroy(): void {
    this.loggedOutSubscription.unsubscribe();
  }
}
