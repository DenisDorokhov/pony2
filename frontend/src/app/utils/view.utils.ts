import {ApplicationRef, ViewContainerRef} from "@angular/core";

export function resolveAppViewContainerRef(applicationRef: ApplicationRef): ViewContainerRef {
  const appInstance = applicationRef.components[0].instance;
  if (!appInstance.viewContainerRef) {
    throw new Error(`Missing 'viewContainerRef' declaration in app component.`);
  }
  return appInstance.viewContainerRef;
}
