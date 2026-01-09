# Fahrenheit Android - TODO List

## Features to Implement

### UI/UX Improvements
- [ ] **Move layout button to settings page**
  - Remove the "Switch to Fluid/Row Layout" button from the main screen top bar
  - Add layout preference option in SettingsActivity
  - Persist layout choice in SharedPreferences
  - Load saved layout preference on app start

### Screensaver & Power Management
- [ ] **Custom screensaver implementation**
  - Prevent Fire Stick default screensaver from starting
  - Create custom screensaver with:
    - Option 1: Rotating book/podcast covers from library
    - Option 2: Now playing information with cover art
    - Option 3: Library statistics visualization
  - Add screensaver settings in SettingsActivity:
    - Enable/disable custom screensaver
    - Screensaver timeout duration
    - Screensaver content type selection
  - Use FLAG_KEEP_SCREEN_ON or similar to manage screen behavior

## Future Enhancements (from previous discussions)

### Performance & Caching
- [ ] **Implement result caching**
  - Cache authors/series/collections API responses
  - Add cache invalidation strategy (time-based or manual)
  - Reduce server calls on repeated navigation
  - Consider implementing proper cache layer with expiration

### Stats Improvements
- [ ] **Stats for podcasts**
  - Check if /api/me/listening-stats accepts libraryId parameter
  - Show library-specific stats when viewing podcast library
  - Add filtering options in stats view

### Real-time Updates
- [ ] **Update home screen on playback**
  - When playing a book/podcast, trigger home screen refresh
  - Update "Continue Listening" section in real-time
  - Consider using event bus or callback mechanism
  - Ensure smooth transition without jarring UI changes

## Known Issues
- [ ] **Authors screen not loading data**
  - API call succeeds (200 OK, 174KB response)
  - Data not displaying in UI
  - Need to debug state management and recomposition
