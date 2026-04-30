import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { Meta } from '@angular/platform-browser';
import { filter, map } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html'
})
export class App implements OnInit {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly meta = inject(Meta);

  // default description used as a fallback when a route does not declare one
  private static readonly DEFAULT_DESCRIPTION =
    'ActaCofrade gestiona los actos de tu hermandad: tareas, decisiones e incidencias.';

  ngOnInit(): void {
    // listen for route changes to keep document meta tags in sync with the active page
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.collectRouteData())
    ).subscribe((data) => {
      const description = (data['description'] as string | undefined) || App.DEFAULT_DESCRIPTION;
      this.meta.updateTag({ name: 'description', content: description });
    });
  }

  // walks the activated route tree to gather data from the deepest matched route
  private collectRouteData(): Record<string, unknown> {
    let route = this.activatedRoute;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route.snapshot.data;
  }
}
