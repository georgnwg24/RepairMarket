import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatingsCreateEditComponent } from './ratings-create-edit.component';

describe('RatingsCreateEditComponent', () => {
  let component: RatingsCreateEditComponent;
  let fixture: ComponentFixture<RatingsCreateEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingsCreateEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatingsCreateEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
