"""Create Firebase metadata for CI builds that do not connect to production."""
import json
from pathlib import Path

target = Path("app/src/debug/google-services.json")
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({
    "project_info": {"project_number": "123456789012", "project_id": "demo-kalendu"},
    "client": [{
        "client_info": {
            "mobilesdk_app_id": "1:123456789012:android:0123456789abcdef",
            "android_client_info": {"package_name": "com.example.studentcalendar"},
        },
        "oauth_client": [{
            "client_type": 3,
            "client_id": "123456789012-kalendu-ci.apps.googleusercontent.com",
        }],
        "api_key": [{"current_key": "AIzaSy000000000000000000000000000000000"}],
    }],
    "configuration_version": "1",
}, indent=2) + "\n")
