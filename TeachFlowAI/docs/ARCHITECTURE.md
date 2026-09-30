# TeachFlow Architecture

```text
Voice Command
     |
     v
Speech-to-Intent
     |
     +----> Parameter / Slot Extraction
     |
     v
Learned Flow Retrieval
     |
     v
Current Accessibility UI Tree
     |
     v
Semantic Action Matching
     |
     v
Android Accessibility Action
     |
     v
UI State Verification
     |
  +--+---------+
  |            |
Success      Stuck
  |            |
Next step   Ask user
  |
Credential/payment boundary
  |
Hand control to user
```

## Core components

### 1. Speech-to-Intent
Converts a natural-language command into a normalized intent and parameters.

### 2. Teaching Recorder
Observes Accessibility Service events and stores semantic UI information for user actions.

### 3. Workflow Store
Persists learned workflows locally.

### 4. Semantic Matcher
Matches a stored action against the current accessibility tree.

### 5. Replay Engine
Executes matched actions and waits for the next UI state.

### 6. Recovery
When a target cannot be found, the system should ask a specific question rather than blindly tapping.

### 7. Safety Boundary
Authentication, OTP, password and payment screens must stop automation and return control to the user.