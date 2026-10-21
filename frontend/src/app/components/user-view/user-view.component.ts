import {Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {UserService} from '../../services/user.service';
import {ToastrService} from 'ngx-toastr';
import {UserListDto} from '../../dtos/user';
import {ConfirmBanDialogComponent} from "../confirmation-dialogues/confirm-ban-dialog/confirm-ban-dialog.component";
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {Page} from "../../dtos/page";
import {Paginator, PaginatorState} from "primeng/paginator";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-user-view',
  standalone: true,
  imports: [CommonModule, ConfirmBanDialogComponent, ReactiveFormsModule, Paginator],
  templateUrl: './user-view.component.html',
  styleUrls: ['./user-view.component.scss']
})
export class UserViewComponent implements OnInit {
  pageLimit = 10;
  usersPage: Page<UserListDto> = { content: [], totalElements: 0, offset: 0, pageSize: this.pageLimit };
  bannerError: string | null = null;
  selectedUserForBan: UserListDto | null = null;
  error = false;
  loading = false;
  errorMessage = '';
  searchControl = new FormControl('');

  @ViewChild('confirmBanDialog') confirmBanDialog!: ConfirmBanDialogComponent;

  constructor(
    private userService: UserService,
    private notification: ToastrService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.usersPage.offset = +params['offset'] || 0;
      this.usersPage.pageSize = +params['limit'] || this.pageLimit;

      const term = params['search'] || '';
      this.searchControl.setValue(term, { emitEvent: false });

      this.reloadUsers(term);
    });

    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(term => {
        this.usersPage.offset = 0;
        const queryParams: any = {
          offset: 0,
          limit: this.usersPage.pageSize
        };
        if (term?.trim()) {
          queryParams.search = term.trim();
        }

        this.reloadUsers(term);
      });
  }

  reloadUsers(term?: string): void {
    this.loading = true;
    let offset = this.usersPage.offset;
    let limit = this.usersPage.pageSize;

    this.userService.getPageOfUsers(offset, limit, term).subscribe({
      next: page => {
        this.usersPage = page;
        this.bannerError = null;
        this.loading = false;
      },
      error: err => {
        console.error('Error loading users', err);
        this.bannerError = err?.error?.message || 'Error loading users';
        this.loading = false;
      }
    });
  }

  /*deleteUser(user: UserListDto): void {
    this.userService.deleteUser(user.username).subscribe({
      next: () => {
        this.notification.success(`Deleted user ${user.username}`);
        this.reloadUsers();
      },
      error: err => {
        console.error('Error deleting user', err);
        const message = err.error?.message || err.message || 'Unexpected error';
        this.notification.error(message, 'Failed to delete user', {
          timeOut: 10000,
          enableHtml: true
        });
        this.error = true;
        this.errorMessage = message;
      }
    });
  }*/

  /*confirmBan(user: UserListDto): void {
    const action = user.banned ? 'Unban' : 'Ban';
    const ok = window.confirm(
      `Are you sure you want to ${action.toLowerCase()} ${user.username}?`
    );
    if (ok) {
      this.toggleBan(user);
    }
  }*/

  confirmBan(user: UserListDto): void {
    this.selectedUserForBan = user;
    this.confirmBanDialog.banWhat = user.username;
    this.confirmBanDialog.isBanned = user.banned;
  }

  onBanConfirm(): void {
    if (!this.selectedUserForBan) {
      return;
    }
    this.toggleBan(this.selectedUserForBan);
  }

  private toggleBan(user: UserListDto): void {
    const op$ = user.banned
      ? this.userService.unbanUser(user.id)
      : this.userService.banUser(user.id);

    op$.subscribe({
      next: () => {
        const verb = user.banned ? 'unbanned' : 'banned';
        this.notification.success(`User ${verb} successfully`, 'Success');
        this.reloadUsers(this.searchControl.value);
      },
      error: err => {
        const verb = user.banned ? 'unban' : 'ban';
        const msg = err.error?.message || err.message || `Could not ${verb} user`;
        this.notification.error(msg, 'Error');
      }
    });
  }

  trackById(index: number, item: UserListDto): number {
    return item.id;
  }

  onPageChange(event: PaginatorState): void {
    this.usersPage.offset = event.first ?? 0;
    this.usersPage.pageSize = event.rows ?? this.pageLimit;
    const queryParams: any = {
      offset: this.usersPage.offset,
      limit: this.usersPage.pageSize
    };
    const searchTerm = this.searchControl.value?.trim();
    if (searchTerm) {
      queryParams.search = searchTerm;
    }

    this.reloadUsers(this.searchControl.value);
  }

}
