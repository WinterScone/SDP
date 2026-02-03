In this project, there are two user classes: admins and patients. Admins comprises super admins (root) and admins (non-root). New admins can only be registered by root. Patients can register themselves.

Each patient needs to link with a practitioner to get prescription. Once a new patient is registered, they are linked to root by default. This indicates they are unassigned. The root needs to relink them to a non-root admin.



Once the prescription is given, it is sent to the patient's account. They can view their prescription on their end.