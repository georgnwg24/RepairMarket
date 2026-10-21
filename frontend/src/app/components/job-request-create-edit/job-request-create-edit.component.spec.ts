import {fakeAsync, flushMicrotasks} from '@angular/core/testing';
import {of, throwError} from 'rxjs';
import {JobRequestCreateEditComponent} from './job-request-create-edit.component';

describe('Job image upload recovery', () => {
  it('retries only the failed image without creating another job', fakeAsync(() => {
    const job = {id: 42, title: 'Repair'};
    const service = jasmine.createSpyObj('JobRequestService', ['create', 'update', 'uploadImage']);
    service.create.and.returnValue(of(job));
    service.update.and.returnValue(of(job));
    let attempts = 0;
    service.uploadImage.and.callFake(() => ++attempts === 2
      ? throwError(() => new Error('temporary upload failure'))
      : of({id: attempts, displayPosition: attempts - 1}));
    const router = jasmine.createSpyObj('Router', ['navigate']);
    router.navigate.and.returnValue(Promise.resolve(true));
    const notification = jasmine.createSpyObj('ToastrService', ['success']);
    const component = new JobRequestCreateEditComponent(service, {} as any, router, notification, {} as any,
      {detectChanges: () => {}} as any);
    const first = new File(['first'], 'first.png', {type: 'image/png'});
    const second = new File(['second'], 'second.png', {type: 'image/png'});
    component.selectedFiles = [first, second];
    component.previewUrls = ['data:first', 'data:second'];

    void component.onSubmit({valid: true} as any);
    void component.onSubmit({valid: true} as any); // double click while upload is pending
    flushMicrotasks();
    expect(service.create).toHaveBeenCalledTimes(1);
    expect(component.jobId).toBe(42);
    expect(component.selectedFiles).toEqual([second]);
    expect(component.saving).toBeFalse();

    void component.onSubmit({valid: true} as any);
    flushMicrotasks();
    expect(service.create).toHaveBeenCalledTimes(1);
    expect(service.update).toHaveBeenCalledTimes(1);
    expect(service.uploadImage.calls.count()).toBe(3);
    expect(service.uploadImage.calls.mostRecent().args[1]).toBe(second);
    expect(component.selectedFiles).toEqual([]);
    expect(router.navigate).toHaveBeenCalledTimes(1);
  }));
});
