# Insured Asset Documentation

This document describes the Insured Asset entity, the UI interactions for create and update, and how the UI calls the service/DAO layer for updates. It focuses only on the insured asset flow in the JavaFX UI.

## Entity Overview

**Entity:** `tn.esprit.entities.InsuredAsset`

**Fields**
- `id` (int) - database primary key
- `name` (String)
- `type` (String)
- `value` (double)
- `description` (String)
- `createdAt` (LocalDateTime)
- `location` (String)
- `userId` (int) - foreign key referencing `user.id`

**Constructors**
- `InsuredAsset()`
- `InsuredAsset(int id, String name, String type, double value, String description, LocalDateTime createdAt, String location, int userId)`
- `InsuredAsset(String name, String type, double value, String description, String location, int userId)`

## UI Layout (Main.fxml)

The insured asset UI is split into two tabs in `View/Main.fxml`:

1. **Form tab** (edit/create)
   - `TextField` inputs for `name`, `type`, `value`, `description`, `location`, `userId`
   - `DatePicker` for `createdAt`
   - `Button` `SubAsset` for submit/update

2. **Asset List tab**
   - `TableView` `assetsTable` with columns for all fields
   - A hidden `ID` column (`colId`) used internally for selection
   - `Update` and `Delete` buttons, disabled until a row is double-clicked

Relevant FXML ids:
- Form fields: `AssName`, `AssType`, `AssValue`, `AssDesc`, `AssCreated`, `AssLocation`, `AssUser`
- Submit button: `SubAsset`
- Table: `assetsTable`
- Columns: `colId`, `colName`, `colType`, `colValue`, `colDescription`, `colCreatedAt`, `colLocation`, `colUserId`
- Buttons: `updateAssetButton`, `deleteAssetButton`
- Tabs: `mainTabs`, `assetFormTab`, `assetListTab`

## Controller Overview (MainController)

**Controller class:** `controller.MainController`

The controller manages:
- Form validation and parsing
- Create vs update mode using a boolean flag
- Table population and double-click selection
- Switching between tabs for editing

### Key Controller State

- `selectedAsset` - the asset selected in the table (set on double-click)
- `isEditMode` - controls whether submit performs insert or update

## UI Interaction Flow

### A) Creating a New Insured Asset

1. User opens the form tab and fills in fields.
2. User clicks `Submit`.
3. The controller validates required fields and parses numeric values.
4. The controller verifies `userId` exists via `UserDAO.read(userId)`.
5. Because `isEditMode == false`, the controller **creates** a new asset and calls:
   - `InsuredAssetDAO.create(asset)`
6. On success:
   - A success alert is shown
   - The form is cleared
   - The table is refreshed
   - The UI returns to the list tab

### B) Editing an Existing Asset (Update)

1. User goes to the Asset List tab.
2. User **double-clicks** a row in the table.
   - This sets `selectedAsset`
   - Enables the `Update`/`Delete` buttons
3. User clicks **Update**.
   - Controller fills the form fields with the selected asset data, including `location`
   - Switches to the form tab
   - Sets `isEditMode = true` and changes the button text to "Update"
4. User edits the values and clicks `Update` (same button).
5. Because `isEditMode == true`, the controller **updates** the asset and calls:
   - `InsuredAssetDAO.update(asset)`
6. On success:
   - A success alert is shown
   - The form is cleared
   - The table is refreshed
   - The UI returns to the list tab
   - `isEditMode` is reset to `false`

## Boolean Approach (Submit vs Update)

The submit button is used for **both** create and update actions. The flow is controlled by a boolean flag:

- `isEditMode = false`  -> submit performs **insert**
- `isEditMode = true`   -> submit performs **update**

The flag is set to `true` only when:
- A row is double-clicked
- The user presses the `Update` button
- The form is populated with the selected asset

The flag is reset to `false` when:
- The table is refreshed
- The form is cleared after a successful submit/update

## UI to Service/DAO Interaction (Update Only)

**Update path:**

1. User double-clicks a row in the table
2. User clicks **Update**
3. Controller populates form and switches to form tab
4. User edits and submits
5. Controller validates inputs and parses values
6. Controller builds an `InsuredAsset` with the existing `id`
7. Controller calls `InsuredAssetDAO.update(asset)`
8. Database update is executed
9. UI refreshes the table

This flow ensures the update always uses the original `id` while allowing all other fields (including `location`) to be changed.

## Notes and Constraints

- The `userId` must exist in the `user` table due to the foreign key constraint.
- The ID column is hidden in the UI but used internally.
- The Update/Delete buttons are disabled until a row is double-clicked.
- `createdAt` uses the selected date from the `DatePicker` when provided.
- `location` is a required text field in the form and is displayed in the table.

## Related Files

- `src/main/java/tn/esprit/entities/InsuredAsset.java`
- `src/main/java/tn/esprit/dao/InsuredAssetDAO.java`
- `src/main/java/controller/MainController.java`
- `src/main/resources/View/Main.fxml`
